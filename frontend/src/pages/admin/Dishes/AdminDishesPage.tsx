import { useEffect, useId, useRef, useState } from "react";
import { Ellipsis, Plus } from "lucide-react";
import { adminDishesApi, type AdminCategory, type AdminDishRow } from "../../../api/admin-dishes.api";
import { getApiError } from "../../../api/error";
import { AppFeedbackModal } from "../../../components/ui/AppFeedbackModal";
import { Badge, type BadgeVariant } from "../../../components/ui/Badge";
import { Button } from "../../../components/ui/Button";
import { DataTable, DataTableText, type DataTableColumn } from "../../../components/ui/DataTable";
import { FilterToolbar } from "../../../components/ui/FilterToolbar";
import { FormField } from "../../../components/ui/FormField";
import { IconButton } from "../../../components/ui/IconButton";
import { Select } from "../../../components/ui/Select";
import { Toast } from "../../../components/ui/Toast";
import type { PageResponse } from "../../../types/api";
import type { DishStatus } from "../../../types/menu";
import { DishEditor } from "./DishEditor.tsx";
import "./AdminDishesPage.css";

const PAGE_SIZE = 5;
const price = new Intl.NumberFormat("vi-VN");

const statusOptions: { value: DishStatus; label: string }[] = [
  { value: "AVAILABLE", label: "Còn món" },
  { value: "SOLD_OUT", label: "Hết món" },
  { value: "HIDDEN", label: "Đã ẩn" },
];

const badge: Record<DishStatus, BadgeVariant> = {
  AVAILABLE: "available",
  SOLD_OUT: "sold-out",
  HIDDEN: "hidden",
};

const actions: Record<DishStatus, string> = {
  AVAILABLE: "Hiển thị lại",
  SOLD_OUT: "Đánh dấu hết món",
  HIDDEN: "Ẩn món",
};

const statusHelp: Record<DishStatus, string> = {
  AVAILABLE: "sẽ hiển thị trên thực đơn và khách có thể đặt.",
  SOLD_OUT: "vẫn hiển thị trên thực đơn nhưng tạm ngừng nhận đặt.",
  HIDDEN: "sẽ được ẩn khỏi thực đơn khách hàng.",
};

type Filters = { categoryId: string; status: DishStatus | "" };
type StatusChange = { dish: AdminDishRow; status: DishStatus };

const emptyFilters: Filters = { categoryId: "", status: "" };

export function AdminDishesPage() {
  const [draft, setDraft] = useState<Filters>(emptyFilters);
  const [query, setQuery] = useState({ ...emptyFilters, page: 0 });
  const [result, setResult] = useState<PageResponse<AdminDishRow>>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [categoryError, setCategoryError] = useState("");
  const [revision, reload] = useState(0);
  const [editor, setEditor] = useState<"new" | number | null>(null);
  const [change, setChange] = useState<StatusChange | null>(null);
  const [changing, setChanging] = useState(false);
  const [changeError, setChangeError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    const request = new AbortController();

    adminDishesApi.categories(request.signal)
      .then((data) => {
        setCategories(data);
        setCategoryError("");
      })
      .catch((cause: unknown) => {
        if (!request.signal.aborted) setCategoryError(getApiError(cause).message);
      });

    return () => request.abort();
  }, [revision]);

  useEffect(() => {
    const request = new AbortController();

    adminDishesApi.list({
      page: query.page,
      size: PAGE_SIZE,
      categoryId: query.categoryId ? Number(query.categoryId) : undefined,
      status: query.status || undefined,
    }, request.signal).then((data) => {
      // Số trang có thể giảm khi một món rời khỏi bộ lọc hiện tại.
      const lastPage = Math.max(0, data.totalPages - 1);
      if (query.page > lastPage) {
        setQuery({ ...query, page: lastPage });
        return;
      }

      setResult(data);
      setLoading(false);
    }).catch((cause: unknown) => {
      if (!request.signal.aborted) {
        setError(getApiError(cause).message);
        setLoading(false);
      }
    });

    return () => request.abort();
  }, [query, revision]);

  function refresh() {
    setLoading(true);
    setError("");
    reload((value) => value + 1);
  }

  function updateQuery(next: typeof query) {
    setLoading(true);
    setError("");
    setQuery(next);
  }

  function apply(filters: Filters) {
    updateQuery({ ...filters, page: 0 });
  }

  function clearFilters() {
    setDraft(emptyFilters);
    apply(emptyFilters);
  }

  function goToPage(page: number) {
    updateQuery({ ...query, page });
  }

  function openEditor(value: "new" | number) {
    setNotice("");
    setEditor(value);
  }

  async function confirmStatus() {
    if (!change || changing) return;
    setChanging(true);
    setChangeError("");

    try {
      await adminDishesApi.setStatus(change.dish.id, change.status);
      setChange(null);
      setNotice("Đã cập nhật trạng thái món ăn.");
      refresh();
    } catch (cause) {
      setChangeError(getApiError(cause).message);
    } finally {
      setChanging(false);
    }
  }

  function renderActions(dish: AdminDishRow) {
    return (
      <DishActions
        dish={dish}
        change={(status) => {
          setChangeError("");
          setChange({ dish, status });
        }}
      />
    );
  }

  const columns: DataTableColumn<AdminDishRow>[] = [
   {
  id: "name",
  header: "Món ăn",
  width: 292,
  render: (dish) => (
    <button
      type="button"
      className="admin-dishes__name admin-dishes__item-button"
      aria-label={`Sửa món ${dish.name}`}
      onClick={() => openEditor(dish.id)}
    >
      {dish.imageUrl && <img src={dish.imageUrl} alt="" width={48} height={48} />}
      <span className="sage-data-table__text">
        <span className="sage-data-table__primary" title={dish.name}>
          {dish.name}
        </span>
        {!dish.imageUrl && (
          <span className="sage-data-table__secondary">Không có ảnh</span>
        )}
      </span>
    </button>
  ),
},
    {
      id: "category", header: "Danh mục", width: 160,
      render: (dish) => dish.category.name,
    },
    {
      id: "price", header: "Giá bán", width: 140,
      render: (dish) => `${price.format(dish.price)} đ`,
    },
    {
      id: "tag", header: "Nhãn món", width: 230,
      render: (dish) => dish.tag?.name ?? "—",
    },
    {
      id: "status", header: "Trạng thái", width: 154,
      render: (dish) => <DishBadge status={dish.status} />,
    },
    { id: "actions", header: "Thao tác", width: 160, render: renderActions },
  ];

  const rows = result?.content ?? [];
  const hasMobileCards = !loading && !error && rows.length > 0;
  const filtered = Boolean(query.categoryId || query.status);

  return (
    <section className="admin-dishes">
      {notice && <Toast message={notice} onDismiss={() => setNotice("")} />}

      {editor !== null ? (
        <DishEditor
          key={editor}
          id={editor === "new" ? undefined : editor}
          statusOptions={statusOptions}
          close={() => { setEditor(null); refresh(); }}
          saved={() => {
            setNotice(editor === "new" ? "Đã thêm món ăn." : "Đã lưu thay đổi.");
            setEditor(null);
            if (editor === "new") clearFilters();
            refresh();
          }}
        />
      ) : <>
        <header className="admin-dishes__heading">
          <div>
            <h1>Quản lý thực đơn</h1>
            <p>Quản lý món ăn, danh mục và nhãn hiển thị trên thực đơn.</p>
          </div>
          <Button type="button" leadingIcon={<Plus size={20} />} onClick={() => openEditor("new")}>
            Thêm món
          </Button>
        </header>

        <nav className="admin-dishes__tabs" aria-label="Quản lý thực đơn">
          <span aria-current="page">Món ăn</span>
          <button type="button" disabled>Danh mục</button>
          <button type="button" disabled>Nhãn món</button>
        </nav>

        {categoryError && (
          <p role="alert">
            {categoryError} <Button type="button" variant="ghost" onClick={refresh}>Thử lại</Button>
          </p>
        )}

        <FilterToolbar
          loading={loading}
          onApply={() => apply(draft)}
          onClear={clearFilters}
          canClear={Boolean(draft.categoryId || draft.status || filtered)}
          helperText="Lọc món theo danh mục và trạng thái."
          filters={({ disabled }) => <>
            <FormField className="sage-filter-toolbar__field" label="Danh mục" htmlFor="dish-category-filter">
              <Select
                id="dish-category-filter"
                disabled={disabled}
                value={draft.categoryId}
                options={[
                  { value: "", label: "Tất cả danh mục" },
                  ...categories.map(({ id, name }) => ({ value: String(id), label: name })),
                ]}
                onValueChange={(categoryId) => setDraft({ ...draft, categoryId })}
              />
            </FormField>
            <FormField className="sage-filter-toolbar__field" label="Trạng thái" htmlFor="dish-status-filter">
              <Select
                id="dish-status-filter"
                disabled={disabled}
                value={draft.status}
                options={[{ value: "", label: "Tất cả trạng thái" }, ...statusOptions]}
                onValueChange={(status) => setDraft({ ...draft, status: status as Filters["status"] })}
              />
            </FormField>
          </>}
        />

        <DataTable
          columns={columns}
          data={rows}
          getRowKey={(dish) => dish.id}
          className={hasMobileCards ? "admin-dishes__desktop-table" : ""}
          caption="Danh sách món ăn"
          loading={loading}
          loadingRows={PAGE_SIZE}
          error={error || undefined}
          onRetry={refresh}
          itemLabel="món"
          emptyTitle={filtered ? "Không có món phù hợp" : "Chưa có món ăn"}
          emptyDescription={filtered ? "Thử đổi hoặc xóa bộ lọc." : "Chọn Thêm món để tạo món đầu tiên."}
          pagination={{
            page: query.page,
            size: PAGE_SIZE,
            totalElements: result?.totalElements ?? 0,
            onPageChange: goToPage,
          }}
        />

        {hasMobileCards && result && (
          <div className="admin-dishes__cards">
            {rows.map((dish) => (
              <article key={dish.id} className="admin-dishes__panel admin-dishes__item">
                    <button
                        type="button"
                        className="admin-dishes__item-button"
                        aria-label={`Sửa món ${dish.name}`}
                        onClick={() => openEditor(dish.id)}
                    >
                        <strong>{dish.name}</strong>
                    </button>

                    <DishBadge status={dish.status} />

                    <p>
                        {dish.category.name} · {price.format(dish.price)} đ
                        <br />
                        {dish.tag?.name ?? "—"}
                    </p>

                    {renderActions(dish)}
                    </article>
            ))}
            <p>
              Hiển thị {result.page * result.size + 1}–{result.page * result.size + rows.length}
              {" "}trong {result.totalElements} món
            </p>
            {!result.first && (
              <Button type="button" variant="outline" onClick={() => goToPage(result.page - 1)}>
                Trang trước
              </Button>
            )}
            {!result.last && (
              <Button type="button" variant="outline" onClick={() => goToPage(result.page + 1)}>
                Trang tiếp theo →
              </Button>
            )}
          </div>
        )}

        <small>Món đã ẩn không hiển thị trên thực đơn khách hàng.</small>

        {change && (
          <AppFeedbackModal
            open
            onClose={() => { if (!changing) setChange(null); }}
            title={`${actions[change.status]}?`}
            description={`${change.dish.name} ${statusHelp[change.status]}`}
            severity={change.status === "HIDDEN" ? "destructive" : "confirm"}
            primaryVariant={change.status === "HIDDEN" ? "destructive" : "primary"}
            primaryButtonLabel={actions[change.status]}
            secondaryButtonLabel="Quay lại"
            onPrimaryAction={() => void confirmStatus()}
            loading={changing}
          >
            {changeError && <p role="alert">{changeError}</p>}
          </AppFeedbackModal>
        )}
      </>}
    </section>
  );
}

function DishBadge({ status }: { status: DishStatus }) {
  return <Badge variant={badge[status]} label={statusOptions.find(({ value }) => value === status)?.label} />;
}

function DishActions({ dish, change }: {
  dish: AdminDishRow;
  change: (status: DishStatus) => void;
}) {
  const id = useId();
  const menu = useRef<HTMLDivElement>(null);

  return (
    <div className="admin-dishes__actions">
      <IconButton
        className="admin-dishes__more"
        icon={<Ellipsis size={24} />}
        popoverTarget={id}
        aria-label={`Thao tác với ${dish.name}`}
      />

      <div ref={menu} id={id} popover="auto" className="admin-dishes__popover">
        <small>{dish.name}</small>
        {statusOptions
          .filter(({ value }) => value !== dish.status)
          .map(({ value }) => (
            <Button
              key={value}
              type="button"
              variant="ghost"
              onClick={() => {
                menu.current?.hidePopover();
                change(value);
              }}
            >
              {actions[value]}
            </Button>
          ))}
      </div>
    </div>
  );
}