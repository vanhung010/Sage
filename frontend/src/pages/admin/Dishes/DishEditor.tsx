import { useEffect, useRef, useState, type FormEvent } from "react";
import { useBlocker } from "react-router-dom";
import { adminDishesApi, type AdminCategory, type AdminDish } from "../../../api/admin-dishes.api";
import { getApiError } from "../../../api/error";
import { Button } from "../../../components/ui/Button";
import { FormField } from "../../../components/ui/FormField";
import { Input } from "../../../components/ui/Input";
import { Select } from "../../../components/ui/Select";
import { Textarea } from "../../../components/ui/Textarea";
import type { DishStatus, Tag } from "../../../types/menu";

type EditorProps = {
  id?: number;
  statusOptions: { value: DishStatus; label: string }[];
  close: () => void;
  saved: () => void;
};

type EditorData = {
  dish: AdminDish | null;
  categories: AdminCategory[];
  tags: Tag[];
};

const leaveMessage = "Rời trang và bỏ các thay đổi chưa lưu?";

export function DishEditor({ id, statusOptions, close, saved }: EditorProps) {
  const [data, setData] = useState<EditorData>();
  const [error, setError] = useState("");
  const [attempt, retry] = useState(0);

  useEffect(() => {
    const request = new AbortController();
    const signal = request.signal;

    Promise.all([
      id === undefined ? Promise.resolve(null) : adminDishesApi.detail(id, signal),
      adminDishesApi.categories(signal),
      adminDishesApi.tags(signal),
    ]).then(([dish, categories, tags]) => setData({ dish, categories, tags }))
      .catch((cause: unknown) => {
        if (!signal.aborted) setError(getApiError(cause).message);
      });

    return () => request.abort();
  }, [id, attempt]);

  return <>
    <header>
      <h1>{id === undefined ? "Thêm món ăn" : "Chỉnh sửa món ăn"}</h1>
      <p>{id === undefined
        ? "Tạo món mới và chọn danh mục trước khi lưu."
        : "Cập nhật thông tin và cách món ăn xuất hiện trên thực đơn."}</p>
    </header>
    {data ? (
      <DishForm {...data} statusOptions={statusOptions} close={close} saved={saved} />
    ) : <>
      <Button type="button" variant="ghost" onClick={close}>← Quay lại thực đơn</Button>
      {error ? (
        <p role="alert">
          {error} <Button type="button" variant="outline" onClick={() => {
            setError("");
            retry((value) => value + 1);
          }}>Thử lại</Button>
        </p>
      ) : <p role="status">Đang tải biểu mẫu…</p>}
    </>}
  </>;
}

function DishForm({
  dish, categories, tags, statusOptions, close, saved,
}: EditorData & Omit<EditorProps, "id">) {
  const [original, setOriginal] = useState(dish);
  const [tagIds, setTagIds] = useState(dish?.tags.map(({ id }) => id) ?? []);
  const [image, setImage] = useState<File>();
  const [preview, setPreview] = useState("");
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const fileInput = useRef<HTMLInputElement>(null);
  const blocker = useBlocker(dirty || saving);

  useEffect(() => {
    if (blocker.state !== "blocked") return;
    if (!saving && window.confirm(leaveMessage)) blocker.proceed();
    else blocker.reset();
  }, [blocker, saving]);

  useEffect(() => {
    if (!dirty && !saving) return;
    function warn(event: BeforeUnloadEvent) { event.preventDefault(); }
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty, saving]);

  useEffect(() => () => {
    if (preview) URL.revokeObjectURL(preview);
  }, [preview]);

  function cancel() {
    if (!saving && (!dirty || window.confirm(leaveMessage))) close();
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;

    const data = new FormData(event.currentTarget);
    const name = String(data.get("name")).trim();

    if (!name || !data.get("categoryId")) {
      setError("Hãy nhập tên món và chọn danh mục.");
      return;
    }

    if (image && (!image.type.startsWith("image/") || !image.size || image.size > 5 * 1024 * 1024)) {
      setError("Hãy chọn tệp hình ảnh hợp lệ, tối đa 5 MB.");
      return;
    }

    const status = data.get("status") as DishStatus;
    data.set("name", name);
    data.set("price", String(Number(data.get("price"))));
    data.set("displayOrder", String(Number(data.get("displayOrder"))));

    // Spring bind "1,2" thành [1, 2] và chuỗi rỗng thành [].
    data.set("tagIds", [...tagIds].sort((a, b) => a - b).join(","));
    if (image) data.set("image", image);

    setSaving(true);
    setError("");
    let detailsSaved = false;

    try {
      if (!original) {
        await adminDishesApi.create(data);
      } else {
        // PATCH thông tin chỉ gửi trường đã thay đổi. Status có endpoint riêng.
        data.delete("status");
        const previous = {
          name: original.name,
          categoryId: String(original.category.id),
          price: String(original.price),
          displayOrder: String(original.displayOrder),
          description: original.description ?? "",
          tagIds: original.tags.map(({ id }) => id).sort((a, b) => a - b).join(","),
        };

        for (const [field, value] of Object.entries(previous)) {
          if (data.get(field) === value) data.delete(field);
        }

        if (!data.keys().next().done) {
          // Giữ kết quả server để lần thử lại không gửi lại thông tin và ảnh đã lưu.
          setOriginal(await adminDishesApi.update(original.id, data));
          detailsSaved = true;
          setImage(undefined);
          setPreview("");
          if (fileInput.current) fileInput.current.value = "";
        }

        if (status !== original.status) {
          await adminDishesApi.setStatus(original.id, status);
        }
      }
      saved();
    } catch (cause) {
      const message = getApiError(cause).message;
      setError(detailsSaved
        ? `Thông tin món đã lưu. Cập nhật trạng thái thất bại. ${message}`
        : message);
    } finally {
      setSaving(false);
    }
  }

  const imageSrc = image ? preview : original?.imageUrl;
  const categoryOptions = categories
    .filter(({ id, active }) => active || id === dish?.category.id)
    .map(({ id, name, active }) => ({
      value: String(id),
      label: active ? name : `${name} (ngừng dùng)`,
    }));

  return <>
    <Button type="button" variant="ghost" disabled={saving} onClick={cancel}>
      ← Quay lại thực đơn
    </Button>

    <form onSubmit={submit} onChange={() => setDirty(true)}>
      <fieldset disabled={saving} className="admin-dishes__form">
        <section className="admin-dishes__panel">
          <h2>Thông tin món ăn</h2>
          <p>Các trường có dấu * là bắt buộc.</p>
          <hr />

          <FormField label="Tên món ăn" htmlFor="dish-name" required>
            <Input id="dish-name" name="name" required maxLength={255}
              placeholder="Ví dụ: Cá hồi áp chảo" defaultValue={dish?.name} />
          </FormField>

          <FormField label="Danh mục" htmlFor="dish-category" required>
            <Select id="dish-category" name="categoryId" options={categoryOptions}
              defaultValue={dish ? String(dish.category.id) : ""}
              placeholder="Chọn danh mục đang hoạt động"
              onValueChange={() => setDirty(true)} />
          </FormField>

          <div className="admin-dishes__pair">
            <FormField label="Giá bán (đ)" htmlFor="dish-price" required>
              <Input id="dish-price" name="price" type="number" required
                placeholder="Nhập số tiền" min={1} max={2147483647} step={1}
                defaultValue={dish?.price} />
            </FormField>
            <FormField label="Thứ tự hiển thị" htmlFor="dish-order" supportingText="Số nhỏ được hiển thị trước.">
              <Input id="dish-order" name="displayOrder" type="number"
                min={0} max={2147483647} step={1} defaultValue={dish?.displayOrder ?? 0} />
            </FormField>
          </div>

          <FormField label="Mô tả" htmlFor="dish-description">
            <Textarea id="dish-description" name="description"
              placeholder="Giới thiệu nguyên liệu và hương vị của món"
              defaultValue={dish?.description ?? ""} />
          </FormField>

          <FormField label="Nhãn món" supportingText="Có thể chọn nhiều nhãn hoặc để trống.">
            <details className="admin-dishes__tags">
              <summary>
                {tags.filter(({ id }) => tagIds.includes(id)).map(({ name }) => name).join(" · ")
                  || "Chọn nhãn món"}
              </summary>
              {tags.length === 0 && <p>Chưa có nhãn món.</p>}
              {tags.map(({ id, name }) => (
                <label key={id}>
                  <input type="checkbox" checked={tagIds.includes(id)} onChange={(event) => {
                    setTagIds(event.target.checked
                      ? [...tagIds, id]
                      : tagIds.filter((tagId) => tagId !== id));
                  }} /> {name}
                </label>
              ))}
            </details>
          </FormField>
        </section>

        <div className="admin-dishes__side">
          <section className="admin-dishes__panel">
            <h2>Ảnh món ăn</h2>
            <p>Ảnh minh họa giúp khách dễ lựa chọn.</p>
            <hr />

            <div className={imageSrc ? "admin-dishes__image" : "admin-dishes__image-empty"}>
              {imageSrc
                ? <img className="admin-dishes__preview" src={imageSrc} alt="Ảnh xem trước của món ăn" />
                : <p>Chưa chọn ảnh</p>}
              <Button type="button" variant="outline" onClick={() => fileInput.current?.click()}>
                {imageSrc ? "Thay ảnh" : "Chọn ảnh từ máy"}
              </Button>
            </div>

            <input ref={fileInput} type="file" accept="image/*" hidden onChange={(event) => {
              const file = event.target.files?.[0];
              setImage(file);
              setPreview(file ? URL.createObjectURL(file) : "");
            }} />
            <small>Tệp ảnh tối đa 5 MB.</small>
          </section>

          <section className="admin-dishes__panel">
            <h2>Hiển thị trên thực đơn</h2>
            <p>Chọn trạng thái phù hợp khi lưu.</p>
            <hr />
            <FormField label="Trạng thái" htmlFor="dish-status">
              <Select id="dish-status" name="status" options={statusOptions}
                defaultValue={dish?.status ?? "AVAILABLE"} onValueChange={() => setDirty(true)} />
            </FormField>
          </section>
        </div>
      </fieldset>

      {error && <p role="alert" className="admin-dishes__error">{error}</p>}

      <footer className="admin-dishes__footer">
        <p>Các thay đổi được áp dụng sau khi lưu.</p>
        <Button type="button" variant="outline" disabled={saving} onClick={cancel}>Hủy</Button>
        <Button type="submit" loading={saving}>{dish ? "Lưu thay đổi" : "Tạo món"}</Button>
      </footer>
    </form>
  </>;
}