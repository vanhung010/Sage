// Start the Postman simulation documented in postman/README.md first.
import assert from "node:assert/strict";
import { createServer } from "vite";

// Uses the application's real Axios client over HTTP, with a synthetic token.
process.env.VITE_API_BASE_URL = "http://127.0.0.1:4900/sage";
globalThis.localStorage = { getItem: () => "local-staff-fixture" };
const server = await createServer({ server: { middlewareMode: true }, appType: "custom" });

try {
  const { staffReservationApi } = await server.ssrLoadModule("/src/api/staff-reservation.api.ts");
  const { StaffReservationDetailView } = await server.ssrLoadModule("/src/pages/staff/ReservationDetail/StaffReservationDetailPage.tsx");
  const { reservationNotice, reservationTimeline, DETAIL_ACTIONS, cleanupEndsAt, detailDateTime } =
    await server.ssrLoadModule("/src/pages/staff/ReservationDetail/reservationDetailPresentation.ts");
  const { createElement } = await import("react");
  const { renderToStaticMarkup } = await import("react-dom/server");
  const { MemoryRouter } = await import("react-router-dom");
  const expected = ["PENDING", "CONFIRMED", "ARRIVED", "COMPLETED", "CANCELLED", "REJECTED", "NO_SHOW"];
  const expectedActions = [4, 4, 2, 0, 0, 0, 0];

  for (const [index, status] of expected.entries()) {
    const reservation = await staffReservationApi.getDetail(index + 1);
    assert.equal(reservation.status, status);
    assert.equal(DETAIL_ACTIONS[status].length, expectedActions[index]);
    const notice = reservationNotice(reservation);
    const events = reservationTimeline(reservation);
    assert.ok(notice.title && notice.description);
    assert.equal(events.length, reservation.logs.length);
    assert.equal(events.at(-1).title, "Đã tạo đơn đặt bàn");
    assert.equal(events.at(-1).description, "Chờ duyệt • 8 khách • B08 • 18:30–20:00");
    const html = renderToStaticMarkup(createElement(MemoryRouter, null,
      createElement(StaffReservationDetailView, { reservation })));
    assert.ok(html.includes(notice.title));
    assert.ok(html.includes("Nhật ký thay đổi"));
    assert.ok(html.includes('href="/staff/reservations"'));
    assert.ok(!html.includes("undefined") && !html.includes("[object Object]"));
    const buttons = html.match(/<button\b[^>]*>/g) || [];
    assert.equal(buttons.length, expectedActions[index] + (status === "CONFIRMED" ? 1 : 0));
    assert.ok(buttons.every((button) => button.includes("disabled")));
    console.log(`PASS ${status}: HTTP, notice, actions, timeline, rendered markup`);
  }

  const confirmed = await staffReservationApi.getDetail(2);
  assert.ok(reservationNotice(confirmed).description.includes("18:45 ngày 24/10/2026"));
  assert.equal(detailDateTime(cleanupEndsAt(confirmed)), "20:15 ngày 24/10/2026");
  const empty = await staffReservationApi.getDetail(8);
  const emptyHtml = renderToStaticMarkup(createElement(MemoryRouter, null,
    createElement(StaffReservationDetailView, { reservation: empty })));
  for (const text of ["Nhân viên đặt hộ", "Chưa có tên khách", "Chưa có số điện thoại", "Không có ghi chú", "Không yêu cầu khu vực", "Chưa có hoạt động"]) {
    assert.ok(emptyHtml.includes(text), text);
  }
  const rescheduled = await staffReservationApi.getDetail(9);
  const history = reservationTimeline(rescheduled);
  assert.equal(history[0].title, "Đã đổi lịch đặt bàn");
  assert.equal(history[0].actorName, "Hệ thống");
  assert.ok(history[0].description.includes("25/10/2026 · 00:30 → 25/10/2026 · 20:00"));
  assert.ok(history[0].description.includes("#2 → B08"));
  assert.ok(history[0].description.includes("Không yêu cầu → Trong nhà"));
  assert.equal(rescheduled.logs[0].eventType, "CREATED", "sorting must not mutate the response");

  for (const id of [403, 404]) {
    await assert.rejects(staffReservationApi.getDetail(id), (error) => error.response?.status === id);
  }
  await assert.rejects(staffReservationApi.getDetail(500), (error) => error.response?.status === 500);
  assert.equal((await staffReservationApi.getDetail(500)).status, "CONFIRMED");
  const controller = new AbortController();
  const pending = staffReservationApi.getDetail(10, controller.signal);
  setTimeout(() => controller.abort(), 100);
  await assert.rejects(pending, (error) => error.code === "ERR_CANCELED");
  console.log("PASS nullable fields, empty history, UTC rollover, sorting, 403/404/500, retry, AbortSignal");
  console.log("Not browser visual verification or live backend validation.");
} finally {
  await server.close();
  delete globalThis.localStorage;
}
