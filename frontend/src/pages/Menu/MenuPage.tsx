import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { menuApi } from "../../api/menu.api";
import { getApiError } from "../../api";
import type { DishSummary } from "../../types/menu";
import type { PriceRange } from "../../types/menu";
import { Search } from "lucide-react";

import { Header } from "../../components/layout/Header";
import {
  Footer,
  type FooterRestaurantInfo,
} from "../../components/layout/Footer";

import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { MenuCategoryTab } from "../../components/ui/MenuCategoryTab";
import { Pagination } from "../../components/ui/Pagination";

import { DishCard } from "../../components/common/DishCard";

import "./MenuPage.css";

/* =========================
   TYPES
   ========================= */

interface Category {
  id: string;
  name: string;
}


/* =========================
   MOCK DATA
   Sau này thay bằng API
   ========================= */

const mockRestaurantInfo: FooterRestaurantInfo = {
  description:
    "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address:
    "123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};

/*
  Các tên category này hiện chỉ dùng để test.
  Sau này lấy từ API.
*/
const mockCategories: Category[] = [
  {
    id: "all",
    name: "Tất cả",
  },
  {
    id: "starter",
    name: "Khai vị",
  },
  {
    id: "main",
    name: "Món chính",
  },
  {
    id: "vegetarian",
    name: "Món chay",
  },
  {
    id: "dessert",
    name: "Tráng miệng",
  },
  {
    id: "drink",
    name: "Đồ uống",
  },
];

const priceOptions = [
  { value: "", label: "Tất cả khoảng giá" },
  { value: "UNDER_100K", label: "Dưới 100.000₫" },
  { value: "FROM_100K_TO_200K", label: "100.000₫ - 200.000₫" },
  { value: "FROM_200K_TO_300K", label: "200.000₫ - 300.000₫" },
  { value: "OVER_300K", label: "Trên 300.000₫" },
];

const tagOptions = [
  {
    value: "best-seller",
    label: "Bán chạy",
  },
  {
    value: "new",
    label: "Món mới",
  },
];


/* =========================
   PAGE
   ========================= */

export function MenuPage() {

  const navigate = useNavigate();

  const [dishes, setDishes] = useState<DishSummary[]>([]);

  const [totalPages, setTotalPages] = useState(0);

  const [loading, setLoading] = useState(false);

  const [error, setError] = useState("");

  const [searchKeyword, setSearchKeyword] = useState("");

  const [debouncedSearch, setDebouncedSearch] = useState("");

   const [currentPage, setCurrentPage] = useState(1);

   const [priceRange, setPriceRange] = useState<PriceRange | "">("");
    const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
    const [selectedTagId, setSelectedTagId] = useState<number | null>(null);

  //explain
  useEffect(() => {
    //explain
    const timer = window.setTimeout(() => {setDebouncedSearch(searchKeyword.trim())}, 300);

    return () => window.clearTimeout(timer);
  }, [searchKeyword]);

  useEffect(() => {
    //explain
    const controller = new AbortController();

    async function loadDishes() {
      setLoading(true);
      setError("");

      //call api
      try {
        const result = await menuApi.listDishes({
          page: currentPage -1,
          search: debouncedSearch || undefined,
          categoryId: selectedCategoryId ?? undefined,
          tagId: selectedTagId ?? undefined,
          priceRange: priceRange || undefined
        }, 
      controller.signal);

        if (!controller.signal.aborted) {
          setDishes(result.content);
          setTotalPages(result.totalPages);
        }
      }

      catch(cause) {
        if(!controller.signal.aborted) {
          setError(getApiError(cause).message);
          setDishes([]);
        }
      }

      finally {
        if(!controller.signal.aborted){
          setLoading(false);
        }
      }
    }

    void loadDishes();

    return () => controller.abort();

  }, [currentPage, debouncedSearch, selectedCategoryId, selectedTagId, priceRange]);


  const [selectedCategory, setSelectedCategory] = useState("all");

  const [selectedTag, setSelectedTag] = useState("");

 

 

  return (
    <div className="menu-page">
      <Header variant="guest" />

      <main className="menu-page__main">
        {/* =====================
            PAGE HEADER
            ===================== */}

        <section className="menu-page__header">
          <div className="container">
            <div className="menu-page__header-content">
              <p className="menu-page__eyebrow">
                THỰC ĐƠN
              </p>

              <h1 className="menu-page__title">
                Thực đơn Sage
              </h1>

              <p className="menu-page__description">
                Khám phá những món ăn được chuẩn bị từ
                nguyên liệu tươi ngon, kết hợp giữa hương
                vị quen thuộc và phong cách hiện đại.
              </p>
            </div>
          </div>
        </section>

        {/* =====================
            MENU CONTENT
            ===================== */}

        <section className="menu-page__content">
          <div className="container menu-page__container">
            {/* Category */}

            <div className="menu-page__controls">
              <div
                className="menu-page__categories"
                aria-label="Danh mục món ăn"
              >
                {mockCategories.map((category) => (
                  <MenuCategoryTab
                    key={category.id}
                    active={
                      selectedCategory === category.id
                    }
                    onClick={() => {
                      setSelectedCategory(category.id);
                      setCurrentPage(1);
                    }}
                  >
                    {category.name}
                  </MenuCategoryTab>
                ))}
              </div>

              {/* Search + Filter */}

              <div className="menu-page__filters">
                <Input
                  size="lg"
                  value={searchKeyword}
                  placeholder="Tìm món theo tên"
                  leadingIcon={<Search />}
                  onChange={(event) => {
                    setSearchKeyword(
                      event.target.value,
                    );

                    setCurrentPage(1);
                  }}
                />

               <Select
                size="lg"
                value={priceRange}
                placeholder="Khoảng giá"
                options={priceOptions}
                onValueChange={(value) => {
                  setPriceRange(value as PriceRange | "");
                  setCurrentPage(1);
                }}
              />

                <Select
                  size="lg"
                  value={selectedTag}
                  placeholder="Nhãn"
                  options={tagOptions}
                  onValueChange={(value) => {
                    setSelectedTag(value);
                    setCurrentPage(1);
                  }}
                />
              </div>
            </div>

            {/* =====================
                DISH GRID
                ===================== */}
             
                {loading && <p role="status">Đang tải món ăn...</p>}
                {error && <p role="alert">{error}</p>}

                {!loading && !error && dishes.length === 0 && (
                  <p>Không tìm thấy món ăn phù hợp.</p>
                )}

                {!loading && !error && dishes.length > 0 && (
                  <div className="menu-page__dish-grid">
                    {dishes.map((dish) => (
                      <DishCard
                        key={dish.id}
                        name={dish.name}
                        description={dish.description ?? ""}
                        price={dish.price}
                        imageUrl={dish.imageUrl ?? undefined}
                        badge={
                          dish.status === "SOLD_OUT"
                            ? "Hết món"
                            : dish.tags?.[0]?.name
                        }
                        onClick={() => navigate(`/menu/${dish.id}`)}
                        onAdd={
                          dish.status === "SOLD_OUT"
                            ? undefined
                            : () => {
                                // Nối giỏ hàng sau
                              }
                        }
                      />
                    ))}
                  </div>
                )}
                              
            {/* =====================
                PAGINATION
                ===================== */}

            <div className="menu-page__pagination">
             {!loading && !error && totalPages > 1 && (
              <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={(page) => {
                setCurrentPage(page);
                window.scrollTo({top: 0, behavior: "smooth"})
              }}
              >
              </Pagination>
             )}
            </div>
          </div>
        </section>
      </main>

      <Footer
        restaurantInfo={mockRestaurantInfo}
      />
    </div>
  );
}