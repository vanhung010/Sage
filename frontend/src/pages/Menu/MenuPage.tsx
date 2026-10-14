import { useState } from "react";
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

interface DishPreview {
  id: number;
  name: string;
  description: string;
  price: number;
  badge?: string;
  imageUrl?: string;
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
  {
    value: "under-200",
    label: "Dưới 200.000₫",
  },
  {
    value: "200-400",
    label: "200.000₫ - 400.000₫",
  },
  {
    value: "over-400",
    label: "Trên 400.000₫",
  },
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

/*
  Figma đang dùng cùng một DishCard làm placeholder.
  Tạo 9 phần tử để test grid 3 x 3.
*/
const mockDishes: DishPreview[] = Array.from(
  { length: 9 },
  (_, index) => ({
    id: index + 1,
    name: "Cá hồi nướng thảo mộc",
    description:
      "Cá hồi tươi nướng cùng thảo mộc, dùng kèm rau củ và sốt chanh leo.",
    price: 320000,
    badge: "Bán chạy",
  }),
);

/* =========================
   PAGE
   ========================= */

export function MenuPage() {
  const [selectedCategory, setSelectedCategory] =
    useState("all");

  const [searchKeyword, setSearchKeyword] =
    useState("");

  const [priceRange, setPriceRange] =
    useState("");

  const [selectedTag, setSelectedTag] =
    useState("");

  const [currentPage, setCurrentPage] =
    useState(1);

  const totalPages = 8;

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
                    setPriceRange(value);
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

            <div className="menu-page__dish-grid">
              {mockDishes.map((dish) => (
                <DishCard
                  key={dish.id}
                  name={dish.name}
                  description={dish.description}
                  price={dish.price}
                  imageUrl={dish.imageUrl}
                  badge={dish.badge}
                  onAdd={() => {
                    console.log(
                      "Thêm món:",
                      dish.id,
                    );
                  }}
                  onClick={() => {
                    console.log(
                      "Xem chi tiết:",
                      dish.id,
                    );
                  }}
                />
              ))}
            </div>

            {/* =====================
                PAGINATION
                ===================== */}

            <div className="menu-page__pagination">
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={(page) => {
                  setCurrentPage(page);

                  window.scrollTo({
                    top: 0,
                    behavior: "smooth",
                  });
                }}
              />
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