import { Link, useNavigate } from "react-router-dom";
import { Clock3, MapPin } from "lucide-react";

import { Header } from "../../components/layout/Header";
import {
  Footer,
  type FooterRestaurantInfo,
} from "../../components/layout/Footer";

import { Button } from "../../components/ui/Button";
import { DishCard } from "../../components/common/DishCard";

import { useState, useEffect } from "react";

import { menuApi } from "../../api/menu.api";
import { getApiError } from "../../api";


import type { DishSummary } from "../../types/menu";

import heroImage from "../../assets/background/background1.webp";
import aboutImage from "../../assets/background/background2.avif";

import "./HomePage.css";

interface OpeningHour {
  label: string;
  time: string;
}

interface RestaurantInfo
  extends FooterRestaurantInfo {
  aboutDescription: string;
  openingHours: OpeningHour[];
}



const mockRestaurantInfo: RestaurantInfo = {
  description:
    "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",

  aboutDescription:
    "Sage là nhà hàng hiện đại mang cảm hứng từ sự giao thoa giữa ẩm thực đương đại và những giá trị gần gũi, quen thuộc. Chúng tôi hướng đến những bữa ăn được chuẩn bị chỉn chu trong một không gian ấm cúng, nơi món ăn, cuộc trò chuyện và những khoảnh khắc đáng nhớ cùng được sẻ chia.",

  address:
    "123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh",

  phone: "028 3822 1234",

  email: "hello@sage.vn",

  openingHours: [
    {
      label: "Thứ 2 – Thứ 6",
      time: "11:00 – 22:00",
    },
    {
      label: "Thứ 7 – Chủ nhật",
      time: "10:00 – 23:00",
    },
  ],
};


export function HomePage() {
  const navigate = useNavigate();

  //danh sách dishes
  const [dishes, setDishes] = useState<DishSummary[]>([]);
  //Lỗi
  const [error, setError] = useState("");
  
  const [loading, setLoading] = useState(false);

  useEffect(() => {

    const controller = new AbortController();
    async function loadDishes(){
      setError("");
      setLoading(true);

      try{
        //Gọi api
        const response = await menuApi.listDishes({size : 3}, controller.signal)
        
        if(!controller.signal.aborted) {
           setDishes(response.content);
        }
      }

      catch(err){
           if(!controller.signal.aborted) {
          setError(getApiError(err).message);
          setDishes([]);
        }
      }

      finally{
      if(!controller.signal.aborted){
                setLoading(false);
              }
      }

    }

    void loadDishes();

    return () => controller.abort();

  },[])

  return (
    <div className="home-page">
      <Header variant="guest" active="home" />

      <main className="home-page__main">
        {/* HERO */}
        <section className="home-hero">
          <div className="container home-hero__container">
            <div className="home-hero__content">
              <p className="home-hero__eyebrow">
                Nhà hàng hiện đại • Đặt bàn trực tuyến
              </p>

              <h1 className="home-hero__title">
                Một bữa ăn đáng nhớ, bắt đầu từ một
                chiếc bàn được giữ sẵn.
              </h1>

              <p className="home-hero__description">
                Sage mang đến trải nghiệm ẩm thực
                hiện đại trong không gian ấm cúng,
                nơi những món ăn tinh tế và những
                khoảnh khắc ý nghĩa được sẻ chia
                trọn vẹn.
              </p>

              <div className="home-hero__actions">
                <Button
                  variant="primary"
                  size="lg"
                  onClick={() =>
                    navigate("/reservation")
                  }
                >
                  Đặt bàn ngay
                </Button>

                <Button
                  variant="outline"
                  size="lg"
                  onClick={() =>
                    navigate("/menu")
                  }
                >
                  Xem thực đơn
                </Button>
              </div>
            </div>

            <div className="home-hero__media">
              <img
                src={heroImage}
                alt="Không gian nhà hàng Sage"
                className="home-hero__image"
              />
            </div>
          </div>
        </section>

        {/* FEATURED MENU */}
        <section className="home-featured">
          <div className="container home-featured__container">
            <div className="home-featured__header">
              <div className="home-featured__title-group">
                <p className="home-featured__eyebrow">
                  THỰC ĐƠN NỔI BẬT
                </p>

                <h2 className="home-featured__title">
                  Món nổi bật tại Sage
                </h2>
              </div>

              <Link
                to="/menu"
                className="home-featured__view-all"
              >
                Xem toàn bộ thực đơn →
              </Link>
            </div>

            <div className="home-featured__grid">
              {dishes.map(
                (dish) => (
                  <DishCard
                    key={dish.id}
                    name={dish.name}
                    description={dish.description ?? ""}
                    imageUrl={dish.imageUrl ?? undefined}
                    price={dish.price}
                    badge={
                          dish.status === "SOLD_OUT"
                            ? "Hết món"
                            : dish.tags?.[0]?.name
                        }
                    onAdd={() => {
                      console.log(
                        "Thêm món:",
                        dish.name,
                      );
                    }}
                   onClick={() => navigate(`/menu/${dish.id}`)}
                  />
                ),
              )}
            </div>
          </div>
        </section>

        {/* ABOUT */}
        <section
          id="about"
          className="home-about"
        >
          <div className="container home-about__container">
            <div className="home-about__media">
              <img
                src={aboutImage}
                alt="Không gian nhà hàng Sage"
                className="home-about__image"
              />
            </div>

            <div className="home-about__content">
              <div className="home-about__heading">
                <p className="home-about__eyebrow">
                  VỀ CHÚNG TÔI
                </p>

                <h2 className="home-about__title">
                  Về Sage
                </h2>
              </div>

              <p className="home-about__description">
                {
                  mockRestaurantInfo.aboutDescription
                }
              </p>

              <div className="home-about__info">
                {/* Opening Hours */}
                <div className="home-about__info-card">
                  <div className="home-about__info-header">
                    <Clock3
                      size={20}
                      strokeWidth={1.8}
                      aria-hidden="true"
                    />

                    <h3>Giờ mở cửa</h3>
                  </div>

                  <div className="home-about__opening-hours">
                    {mockRestaurantInfo.openingHours.map(
                      (item) => (
                        <div
                          key={item.label}
                          className="home-about__opening-row"
                        >
                          <span>
                            {item.label}
                          </span>

                          <span>
                            {item.time}
                          </span>
                        </div>
                      ),
                    )}
                  </div>
                </div>

                {/* Contact */}
                <div className="home-about__info-card">
                  <div className="home-about__info-header">
                    <MapPin
                      size={20}
                      strokeWidth={1.8}
                      aria-hidden="true"
                    />

                    <h3>
                      Địa chỉ & liên hệ
                    </h3>
                  </div>

                  <div className="home-about__contact">
                    <p>
                      {
                        mockRestaurantInfo.address
                      }
                    </p>

                    <a
                      href={`tel:${mockRestaurantInfo.phone}`}
                    >
                      {
                        mockRestaurantInfo.phone
                      }
                    </a>

                    <a
                      href={`mailto:${mockRestaurantInfo.email}`}
                    >
                      {
                        mockRestaurantInfo.email
                      }
                    </a>
                  </div>
                </div>
              </div>
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