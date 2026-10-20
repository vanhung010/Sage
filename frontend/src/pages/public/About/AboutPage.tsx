import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Header } from "../../../components/layout/Header";
import { Footer } from "../../../components/layout/Footer";
import { Button } from "../../../components/ui/Button";
import { NavigationBreadcrumb } from "../../../components/ui/NavigationBreadcrumb";
import heroImage from "../../../assets/background/background1.webp";
import visitImage from "../../../assets/background/background2.avif";
import headerLogo from "../../../assets/order-detail/header-logo.png";
import footerLogo from "../../../assets/order-detail/footer-logo.png";
import addressIcon from "../../../assets/my-orders/address.svg";
import phoneIcon from "../../../assets/my-orders/phone.svg";
import mailIcon from "../../../assets/my-orders/mail.svg";
import mobileMenuIcon from "../../../assets/my-orders/mobile-menu.svg";
import "./AboutPage.css";

const restaurantInfo = {
  description: "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address: "123 Nguyễn Thị Minh Khai,\nQuận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};

const values = [
  {
    title: "Món ăn chỉn chu",
    description: "Sự cân bằng trong hương vị và cách trình bày, để mỗi món ăn đều góp phần làm nên một bữa ăn đáng nhớ.",
  },
  {
    title: "Không gian ấm cúng",
    description: "Một nơi gần gũi để bạn thư giãn, trò chuyện và dành thời gian cho những người đang ngồi cùng bàn.",
  },
  {
    title: "Trọn vẹn sẻ chia",
    description: "Từ bữa ăn thường ngày đến cuộc gặp gỡ đặc biệt, điều ở lại là những khoảnh khắc được cùng nhau tận hưởng.",
  },
];

const visitDetails = [
  { label: "Địa chỉ", value: restaurantInfo.address },
  { label: "Thứ 2 – Thứ 6", value: "11:00 – 22:00" },
  { label: "Thứ 7 – Chủ nhật", value: "10:00 – 23:00" },
  { label: "Điện thoại", value: restaurantInfo.phone, href: "tel:02838221234" },
];

export function AboutPage() {
  const navigate = useNavigate();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  return (
    <div className="about-page">
      <Header active="about" logoSrc={headerLogo} mobileMenuIcon={mobileMenuIcon} />
      <main className="about-page__main">
        <section className="about-page__hero" aria-labelledby="about-title">
          <div className="container about-page__container about-page__hero-content">
            <NavigationBreadcrumb
              items={[{ label: "Trang chủ", to: "/" }, { label: "Về Sage" }]}
              separator={<span className="sage-navigation-breadcrumb__separator" aria-hidden="true">/</span>}
            />
            <div className="about-page__introduction">
              <div className="about-page__heading">
                <p className="about-page__eyebrow">VỀ SAGE</p>
                <h1 id="about-title">Món ngon kết nối,<br />khoảnh khắc ở lại.</h1>
              </div>
              <p className="about-page__lead">Ẩm thực hiện đại, không gian ấm cúng và những bữa ăn dành cho sự sẻ chia.</p>
            </div>
            <figure className="about-page__hero-figure">
              <img className="about-page__hero-image" src={heroImage} alt="Không gian nhà hàng Sage với những bàn ăn bên cửa sổ" fetchPriority="high" />
              <figcaption>Một chiếc bàn, những món ngon và câu chuyện của bạn.</figcaption>
            </figure>
          </div>
        </section>

        <section className="about-page__section about-page__story" aria-labelledby="about-spirit-title">
          <div className="container about-page__container about-page__story-layout">
            <div className="about-page__heading">
              <p className="about-page__eyebrow">TINH THẦN CỦA SAGE</p>
              <h2 id="about-spirit-title" className="about-page__display-title">Chỉn chu trong món ăn.<br />Gần gũi trong từng khoảnh khắc.</h2>
            </div>
            <div className="about-page__story-copy">
              <p>Sage mang cảm hứng từ sự giao thoa giữa ẩm thực đương đại và những giá trị gần gũi, quen thuộc. Chúng tôi hướng đến những bữa ăn được chuẩn bị chỉn chu trong một không gian ấm cúng.</p>
              <p>Dù là một buổi hẹn, bữa cơm cùng gia đình hay cuộc gặp gỡ bạn bè, Sage mong mỗi chiếc bàn đều là nơi món ăn, cuộc trò chuyện và những khoảnh khắc đáng nhớ được sẻ chia.</p>
            </div>
          </div>
        </section>

        <section className="about-page__section about-page__values" aria-labelledby="about-values-title">
          <div className="container about-page__container about-page__values-content">
            <div className="about-page__values-heading">
              <p className="about-page__eyebrow">NHỮNG ĐIỀU GIẢN DỊ</p>
              <h2 id="about-values-title">Những điều Sage trân trọng</h2>
            </div>
            <div className="about-page__values-grid">
              {values.map((value) => (
                <article className="about-page__value-card" key={value.title}>
                  <h3>{value.title}</h3>
                  <p>{value.description}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="about-page__section about-page__visit" aria-labelledby="about-visit-title">
          <div className="container about-page__container about-page__visit-layout">
            <img className="about-page__visit-image" src={visitImage} alt="Không gian ấm cúng với ánh đèn vàng tại Sage" loading="lazy" />
            <div className="about-page__visit-content">
              <p className="about-page__eyebrow">HẸN GẶP TẠI SAGE</p>
              <h2 id="about-visit-title" className="about-page__display-title">Để Sage chuẩn bị<br />một chỗ cho bạn.</h2>
              <p>Chọn một thời gian phù hợp, đặt bàn trước và dành phần còn lại cho những người bạn yêu quý.</p>
              <dl className="about-page__details">
                {visitDetails.map((detail) => (
                  <div key={detail.label}>
                    <dt>{detail.label}</dt>
                    <dd>{detail.href ? <a href={detail.href}>{detail.value}</a> : detail.value}</dd>
                  </div>
                ))}
              </dl>
              <div className="about-page__actions">
                <Button onClick={() => navigate("/reservation")}>Đặt bàn ngay</Button>
                <Button variant="outline" onClick={() => navigate("/menu")}>Xem thực đơn</Button>
              </div>
            </div>
          </div>
        </section>
      </main>
      <Footer
        restaurantInfo={restaurantInfo}
        logoSrc={footerLogo}
        contactIcons={{ address: addressIcon, phone: phoneIcon, mail: mailIcon }}
        mobileSystemLabel="Hệ thống đặt bàn nhà hàng"
      />
    </div>
  );
}
