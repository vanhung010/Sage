import type { FormEventHandler } from "react";
import { useNavigate } from "react-router-dom";
import type { CartItem } from "../../../types/cart";
import type { OrderResponse } from "../../../types/order";
import { MAX_QUANTITY } from "../../../storage/cartStorage";
import { Header } from "../../../components/layout/Header";
import { Footer } from "../../../components/layout/Footer";
import { Button } from "../../../components/ui/Button";
import { IconButton } from "../../../components/ui/IconButton";
import { FormField } from "../../../components/ui/FormField";
import { Input } from "../../../components/ui/Input";
import { Textarea } from "../../../components/ui/Textarea";
import { Badge } from "../../../components/ui/Badge";
import headerLogo from "../../../assets/order-detail/header-logo.png";
import footerLogo from "../../../assets/order-detail/footer-logo.png";
import addressIcon from "../../../assets/order-detail/address.svg";
import phoneIcon from "../../../assets/order-detail/phone.svg";
import mailIcon from "../../../assets/order-detail/mail.svg";
import cartIcon from "../../../assets/order/cart.svg";
import arrowIcon from "../../../assets/order/arrow.svg";
import minusIcon from "../../../assets/order/minus.svg";
import plusIcon from "../../../assets/order/plus.svg";
import trashIcon from "../../../assets/order/trash.svg";
import codIcon from "../../../assets/order/cod.svg";
import successIcon from "../../../assets/order/success.svg";
import "./OrderPage.css";

export interface OrderFieldErrors { address?: string; phone?: string }

interface Props {
  items: CartItem[];
  subtotal: number;
  totalQuantity: number;
  address: string;
  phone: string;
  fieldErrors: OrderFieldErrors;
  error: string;
  submitting: boolean;
  createdOrder: OrderResponse | null;
  onAddressChange: (value: string) => void;
  onPhoneChange: (value: string) => void;
  onQuantityChange: (id: number, value: number) => void;
  onRemove: (id: number) => void;
  onSubmit: FormEventHandler<HTMLFormElement>;
}

const restaurantInfo = {
  description: "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address: "123 Nguyễn Thị Minh Khai,\nQuận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234", email: "hello@sage.vn",
};
const contactIcons = { address: addressIcon, phone: phoneIcon, mail: mailIcon };
const number = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 });
const money = (value: number) => `${number.format(value)}đ`;

export function OrderCheckoutView(props: Props) {
  const navigate = useNavigate();
  const empty = props.items.length === 0;

  return (
    <div className="checkout-page" aria-busy={props.submitting}>
      <Header active="order" logoSrc={headerLogo} />
      {props.createdOrder ? <OrderSuccess order={props.createdOrder} /> : (
        <main>
          <section className="checkout-page__hero">
            <div className="checkout-page__container">
              <p className="checkout-page__eyebrow">ĐẶT MÓN ĂN</p>
              <h1>{empty ? "Đặt món ăn" : "Hoàn tất đơn đặt món"}</h1>
              <p className="checkout-page__subtitle">{empty
                ? "Chọn những món bạn yêu thích từ thực đơn để bắt đầu đơn hàng."
                : "Kiểm tra món đã chọn, nhập thông tin nhận hàng và xác nhận đơn hàng của bạn."}</p>
            </div>
          </section>

          <div className="checkout-page__content">
            <form className="checkout-page__container checkout-page__grid" onSubmit={props.onSubmit} noValidate>
              <div className="checkout-page__column">
                <section className="checkout-page__card">
                  <div className="checkout-page__section-heading">
                    <h2>Món đã chọn</h2>
                    <p>{empty ? "Những món bạn chọn sẽ xuất hiện tại đây." : "Điều chỉnh số lượng hoặc xóa món trước khi đặt hàng."}</p>
                  </div>

                  {empty ? (
                    <div className="checkout-page__empty">
                      <div className="checkout-page__empty-icon"><img src={cartIcon} width={48} height={48} alt="" /></div>
                      <div className="checkout-page__empty-message">
                        <h2>Chưa có món nào trong giỏ</h2>
                        <p>Khám phá thực đơn Sage và thêm món bạn yêu thích.<br />Sau đó, quay lại đây để hoàn tất đơn đặt món.</p>
                      </div>
                      <Button type="button" size="lg" className="checkout-page__explore"
                        trailingIcon={<img src={arrowIcon} width={20} height={20} alt="" />}
                        onClick={() => navigate("/menu")}>Khám phá thực đơn</Button>
                    </div>
                  ) : (
                    <div className="checkout-page__items">
                      {props.items.map((item) => (
                        <article className="checkout-page__item" key={item.dishId}>
                          {item.imageUrl ? <img className="checkout-page__dish-image" src={item.imageUrl} alt={item.name} width={96} height={96} />
                            : <div className="checkout-page__dish-image checkout-page__image-placeholder">Chưa có ảnh</div>}
                          <div className="checkout-page__dish-content">
                            <h3>{item.name}</h3>
                            <p className="checkout-page__price">{money(item.price)}</p>
                            <div className="checkout-page__stepper">
                              <IconButton variant="outline" size="md" aria-label={`Giảm số lượng ${item.name}`}
                                disabled={props.submitting || item.quantity <= 1}
                                icon={<img src={minusIcon} width={20} height={20} alt="" />}
                                onClick={() => props.onQuantityChange(item.dishId, item.quantity - 1)} />
                              <input type="number" min={1} max={MAX_QUANTITY} step={1} value={item.quantity}
                                disabled={props.submitting} aria-label={`Số lượng ${item.name}`}
                                onChange={(event) => props.onQuantityChange(item.dishId, event.currentTarget.valueAsNumber)} />
                              <IconButton variant="outline" size="md" aria-label={`Tăng số lượng ${item.name}`}
                                disabled={props.submitting || item.quantity >= MAX_QUANTITY}
                                icon={<img src={plusIcon} width={20} height={20} alt="" />}
                                onClick={() => props.onQuantityChange(item.dishId, item.quantity + 1)} />
                            </div>
                          </div>
                          <div className="checkout-page__item-actions">
                            <div><p>Tạm tính</p><strong>{money(item.price * item.quantity)}</strong></div>
                            <IconButton size="sm" variant="ghost" aria-label={`Xóa ${item.name} khỏi giỏ`}
                              disabled={props.submitting} icon={<img src={trashIcon} width={16} height={16} alt="" />}
                              onClick={() => props.onRemove(item.dishId)} />
                          </div>
                        </article>
                      ))}
                    </div>
                  )}
                </section>

                {!empty && <>
                  <section className="checkout-page__card">
                    <div className="checkout-page__section-heading"><h2>Thông tin nhận hàng</h2><p>Nhập địa chỉ giao hàng và số điện thoại người nhận.</p></div>
                    <div className="checkout-page__fields">
                      <FormField label="Địa chỉ nhận hàng" htmlFor="delivery-address" required error={props.fieldErrors.address}>
                        <Textarea id="delivery-address" rows={3} required autoComplete="street-address"
                          placeholder="Nhập địa chỉ nhận hàng" disabled={props.submitting}
                          value={props.address} error={Boolean(props.fieldErrors.address)}
                          onChange={(event) => props.onAddressChange(event.target.value)} />
                      </FormField>
                      <FormField label="Số điện thoại nhận hàng" htmlFor="delivery-phone" required error={props.fieldErrors.phone}>
                        <Input id="delivery-phone" type="tel" size="lg" inputMode="numeric" autoComplete="tel" required maxLength={30}
                          placeholder="Nhập số điện thoại người nhận" disabled={props.submitting}
                          value={props.phone} error={Boolean(props.fieldErrors.phone)}
                          onChange={(event) => props.onPhoneChange(event.target.value)} />
                      </FormField>
                    </div>
                  </section>
                  <section className="checkout-page__card checkout-page__payment">
                    <div className="checkout-page__section-heading"><h2>Phương thức thanh toán</h2><p>Đơn hàng hiện chỉ hỗ trợ thanh toán khi nhận hàng.</p></div>
                    <div className="checkout-page__payment-option">
                      <p><img src={codIcon} width={16} height={16} alt="" />Thanh toán khi nhận hàng (COD)</p>
                      <p>Bạn thanh toán cho nhân viên giao hàng khi nhận món.</p>
                    </div>
                  </section>
                </>}
              </div>

              <aside className="checkout-page__card" aria-label="Tóm tắt đơn hàng">
                <h2>Tóm tắt đơn hàng</h2>
                <dl className="checkout-page__summary">
                  <div><dt>Số món</dt><dd>{props.totalQuantity}</dd></div>
                  <div><dt>Tạm tính</dt><dd>{money(props.subtotal)}</dd></div>
                  <div><dt>Phương thức thanh toán</dt><dd>COD</dd></div>
                  <div className="checkout-page__divider" />
                  <div className="checkout-page__total"><dt>Tổng cộng</dt><dd>{money(props.subtotal)}</dd></div>
                </dl>
                <p className="checkout-page__note">{empty
                  ? "Thêm ít nhất một món vào giỏ hàng để tiếp tục đặt món."
                  : "Sau khi xác nhận, Sage sẽ ghi nhận đơn hàng và chuẩn bị giao đến địa chỉ bạn cung cấp."}</p>
                {props.error && <p className="checkout-page__error" role="alert">{props.error}</p>}
                <div className="checkout-page__actions">
                  <Button type="submit" size="lg" fullWidth loading={props.submitting} disabled={empty}>Đặt món</Button>
                  {!empty && <Button type="button" size="lg" variant="outline" fullWidth disabled={props.submitting}
                    onClick={() => navigate("/menu")}>Tiếp tục chọn món</Button>}
                </div>
              </aside>
            </form>
          </div>
        </main>
      )}
      <Footer restaurantInfo={restaurantInfo} logoSrc={footerLogo} contactIcons={contactIcons} />
    </div>
  );
}

function OrderSuccess({ order }: { order: OrderResponse }) {
  const navigate = useNavigate();
  return (
    <main className="checkout-page__success">
      <div className="checkout-page__success-container">
        <img src={successIcon} width={48} height={48} alt="" />
        <div className="checkout-page__success-heading">
          <p>ĐẶT MÓN THÀNH CÔNG</p>
          <h1 id="order-success-title" tabIndex={-1}>Đơn hàng của bạn đã được ghi nhận</h1>
          <p>Đặt hàng thành công. Vui lòng chờ giao hàng.</p>
        </div>
        <section className="checkout-page__card checkout-page__success-card" aria-label={`Thông tin đơn hàng ${order.id}`}>
          <div className="checkout-page__success-card-heading"><h2>Thông tin đơn hàng</h2><Badge variant="neutral">Đã đặt</Badge></div>
          <div className="checkout-page__divider" />
          <section className="checkout-page__success-section">
            <h3>Thông tin nhận hàng</h3>
            <dl>
              <div><dt>Địa chỉ nhận hàng</dt><dd>{order.deleveryAddress}</dd></div>
              <div><dt>Số điện thoại</dt><dd>{order.deliveryPhone}</dd></div>
              <div><dt>Phương thức thanh toán</dt><dd>{order.paymentMethod}</dd></div>
            </dl>
          </section>
          <div className="checkout-page__divider" />
          <section className="checkout-page__success-section">
            <h3>Món đã đặt</h3>
            <ul>{order.orderDishItemResponseList.map((item) => (
              <li key={item.id}><span>{item.nameDish}</span><span>Số lượng: {item.quantity}</span></li>
            ))}</ul>
          </section>
        </section>
        <p className="checkout-page__success-note">Sage đã ghi nhận thông tin nhận hàng. Vui lòng chờ đơn được giao đến địa chỉ bạn đã cung cấp.</p>
        <div className="checkout-page__success-actions">
          <Button type="button" size="lg" onClick={() => navigate(`/orders/${order.id}`)}>Xem chi tiết đơn hàng</Button>
          <Button type="button" size="lg" variant="outline" onClick={() => navigate("/menu")}>Tiếp tục xem thực đơn</Button>
        </div>
      </div>
    </main>
  );
}
