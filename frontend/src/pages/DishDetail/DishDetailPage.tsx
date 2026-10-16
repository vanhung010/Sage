import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ImageOff } from "lucide-react";

import { menuApi } from "../../api/menu.api";
import { getApiError } from "../../api";
import type { DishDetail } from "../../types/menu";

import { Header } from "../../components/layout/Header";
import {
  Footer,
  type FooterRestaurantInfo,
} from "../../components/layout/Footer";
import { NavigationBreadcrumb } from "../../components/ui/NavigationBreadcrumb";
import { Button } from "../../components/ui/Button";

import "./DishDetailPage.css";

const restaurantInfo: FooterRestaurantInfo = {
  description:
    "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address:
    "123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};

const priceFormatter = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

export function DishDetailPage() {
  const { id } = useParams<{ id: string }>();

  const [dish, setDish] = useState<DishDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [imageFailed, setImageFailed] = useState(false);
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    setDish(null);
    setError("");
    setImageFailed(false);

    if (!id || !/^[1-9]\d*$/.test(id)) {
      setError("Mã món ăn không hợp lệ.");
      setLoading(false);
      return;
    }

    setLoading(true);

    async function loadDish(dishId: string) {
      try {
        const result = await menuApi.getDish(
          dishId,
          controller.signal,
        );

        if (!controller.signal.aborted) {
          setDish(result);
        }
      } catch (cause) {
        if (!controller.signal.aborted) {
          setError(
            getApiError(cause).message ||
              "Không thể tải thông tin món ăn.",
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    void loadDish(id);

    return () => controller.abort();
  }, [id, retryCount]);

  const tags = dish?.tags ?? [];
  const soldOut = dish?.status === "SOLD_OUT";

  return (
    <div className="dish-detail-page">
      <Header />

      <main className="dish-detail-page__main">
        <div className="container dish-detail-page__container">
          {loading && (
            <div
              className="dish-detail-page__feedback"
              role="status"
              aria-live="polite"
            >
              <p>Đang tải thông tin món ăn...</p>
            </div>
          )}

          {!loading && error && (
            <div className="dish-detail-page__feedback">
              <p role="alert">{error}</p>

              <div className="dish-detail-page__feedback-actions">
                <button
                  type="button"
                  className="dish-detail-page__retry"
                  onClick={() => {
                    setRetryCount((value) => value + 1);
                  }}
                >
                  Thử lại
                </button>

                <Link
                  to="/menu"
                  className="dish-detail-page__back-link"
                >
                  Về thực đơn
                </Link>
              </div>
            </div>
          )}

          {!loading && !error && dish && (
            <>
              <NavigationBreadcrumb
                items={[
                  {
                    label: "Thực đơn",
                    to: "/menu",
                  },
                  {
                    label: dish.category.name,
                  },
                  {
                    label: dish.name,
                  },
                ]}
              />

              <article
                className="dish-detail-page__content"
                aria-labelledby="dish-detail-title"
              >
                <div className="dish-detail-page__image-wrapper">
                  {dish.imageUrl && !imageFailed ? (
                    <img
                      className="dish-detail-page__image"
                      src={dish.imageUrl}
                      alt={dish.name}
                      width={632}
                      height={500}
                      onError={() => setImageFailed(true)}
                    />
                  ) : (
                    <div className="dish-detail-page__image-placeholder">
                      <ImageOff
                        size={40}
                        strokeWidth={1.5}
                        aria-hidden="true"
                      />

                      <span>Chưa có hình ảnh món ăn</span>
                    </div>
                  )}
                </div>

                <div className="dish-detail-page__information">
                  <div className="dish-detail-page__heading">
                    <p className="dish-detail-page__category">
                      {dish.category.name}
                    </p>

                    <h1
                      id="dish-detail-title"
                      className="dish-detail-page__title"
                    >
                      {dish.name}
                    </h1>
                  </div>

                  {(tags.length > 0 || soldOut) && (
                    <ul
                      className="dish-detail-page__tags"
                      aria-label="Nhãn món ăn"
                    >
                      {soldOut && (
                        <li className="dish-detail-page__tag dish-detail-page__tag--sold-out">
                          Hết món
                        </li>
                      )}

                      {tags.map((tag) => (
                        <li
                          key={tag.id}
                          className="dish-detail-page__tag"
                        >
                          {tag.name}
                        </li>
                      ))}
                    </ul>
                  )}

                  {dish.description?.trim() && (
                    <p className="dish-detail-page__description">
                      {dish.description}
                    </p>
                  )}

                  <p className="dish-detail-page__price">
                    {priceFormatter.format(dish.price)}
                  </p>

                  <Button
                    type="button"
                    variant="primary"
                    size="lg"
                    disabled={soldOut}
                  >
                    Thêm vào giỏ hàng
                  </Button>
                </div>
              </article>
            </>
          )}
        </div>
      </main>

      <Footer restaurantInfo={restaurantInfo} />
    </div>
  );
}
