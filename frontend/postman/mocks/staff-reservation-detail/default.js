/* Local synthetic fixtures from StaffReservationDetailResponse and log snapshots.
 * This does not implement or test backend authorization.
 */
const http = require("http");
const statuses = ["PENDING", "CONFIRMED", "ARRIVED", "COMPLETED", "CANCELLED", "REJECTED", "NO_SHOW"];
const user = { id: 100, fullName: "Nhân viên kiểm thử", role: "STAFF", email: "staff@example.test", phone: "0900000000" };
let serverErrorAttempts = 0;

function fixture(id) {
  const status = statuses[id - 1] || "CONFIRMED";
  const record = {
    id, code: `SAGE-261024-TEST${id}`, status, source: "ONLINE",
    customerName: "Nguyễn Văn Nam", customerPhone: "0901234567", partySize: 8,
    startTime: "2026-10-24T18:30:00+07:00", endTime: "2026-10-24T20:00:00+07:00",
    table: { id: 8, code: "B08", area: { id: 1, name: "Trong nhà" } },
    requestedArea: { id: 1, name: "Trong nhà" },
    specialNote: "Ưu tiên bàn yên tĩnh, tránh vị trí gần lối đi. Có một khách dị ứng hải sản.",
    bufferMinutes: 15, rescheduleCount: 0,
    cancellationReason: status === "CANCELLED" ? "Khách thay đổi kế hoạch" : null,
    rejectionReason: status === "REJECTED" ? "Không thể đáp ứng yêu cầu phục vụ của nhóm trong khung giờ này" : null,
    createdAt: "2026-10-24T10:30:00+07:00", updatedAt: "2026-10-24T11:05:00+07:00",
    approvalDeadline: status === "PENDING" ? "2026-10-24T15:30:00+07:00" : null,
    noShowAllowedAt: status === "CONFIRMED" ? "2026-10-24T18:45:00+07:00" : null,
    logs: [],
  };
  const created = {
    id: 1, eventType: "CREATED", changedAt: record.createdAt,
    actorName: record.customerName, actorRole: "CUSTOMER", oldValue: null,
    newValue: { status: "PENDING", partySize: 8, tableId: 8, requestedAreaId: 1,
      startTime: "2026-10-24T11:30:00Z", endTime: "2026-10-24T13:00:00Z", bufferMinutes: 15 },
    reason: null,
  };
  const chain = status === "PENDING" ? [] : status === "REJECTED" ? [status]
    : status === "CONFIRMED" ? [status] : status === "ARRIVED" ? ["CONFIRMED", status]
      : status === "COMPLETED" ? ["CONFIRMED", "ARRIVED", status] : ["CONFIRMED", status];
  const eventTimes = { CONFIRMED: "11:05", ARRIVED: "18:28", COMPLETED: "20:05", CANCELLED: "17:15", REJECTED: "10:50", NO_SHOW: "18:50" };
  record.logs = [created, ...chain.map((next, index) => ({
    id: index + 2, eventType: "STATUS_CHANGED", changedAt: `2026-10-24T${eventTimes[next]}:00+07:00`,
    actorName: user.fullName, actorRole: "STAFF", oldValue: { status: index ? chain[index - 1] : "PENDING" },
    newValue: { status: next }, reason: next === "CANCELLED" ? record.cancellationReason : next === "REJECTED" ? record.rejectionReason : null,
  }))].reverse();
  record.updatedAt = record.logs[0].changedAt;
  if (id === 8) Object.assign(record, { source: "STAFF_GUEST", customerName: null, customerPhone: null, specialNote: null, requestedArea: null, logs: [] });
  if (id === 9) Object.assign(record, { specialNote: "Ghi chú rất dài để kiểm tra xuống dòng. ".repeat(35), logs: [created, {
    id: 9, eventType: "RESCHEDULED", changedAt: "2026-10-24T12:00:00Z", actorName: null, actorRole: null,
    oldValue: { startTime: "2026-10-24T16:30:00Z", endTime: "2026-10-24T17:30:00Z", partySize: 4, tableId: 2, requestedAreaId: null },
    newValue: { startTime: "2026-10-25T11:30:00Z", endTime: "2026-10-25T13:00:00Z", partySize: 8, tableId: 8, requestedAreaId: 1 }, reason: "Khách đổi lịch",
  }] });
  return record;
}

http.createServer((req, res) => {
  const path = new URL(req.url, "http://localhost").pathname;
  res.setHeader("Access-Control-Allow-Origin", "http://localhost:8001");
  res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  if (req.method === "OPTIONS") { res.writeHead(204); res.end(); return; }
  function send(status, body) { res.writeHead(status, { "Content-Type": "application/json" }); res.end(JSON.stringify(body)); }
  function error(status, code, message) { send(status, { status, code, message }); }
  if (path === "/api/auth/login" && req.method === "POST") {
    return send(200, { accessToken: "local-staff-fixture", tokenType: "Bearer", expiresIn: 3600, user });
  }
  if (path === "/api/auth/me") return send(200, user);
  if (path === "/api/areas") return send(200, [{ id: 1, name: "Trong nhà", description: null }]);
  if (path === "/api/staff/reservations/summary") return send(200, { pendingCount: 1 });
  if (path === "/api/staff/reservations") return send(200, {
    content: statuses.map((status, index) => ({ ...fixture(index + 1), customerName: status, tableId: 8, tableCode: "B08", areaId: 1, areaName: "Trong nhà" })),
    page: 0, size: 7, totalElements: 7, totalPages: 1, first: true, last: true,
  });
  const match = path.match(/^\/api\/staff\/reservations\/(\d+)$/);
  if (req.method === "GET" && match) {
    const id = Number(match[1]);
    if (id === 403) return error(403, "ACCESS_DENIED", "Bạn không có quyền xem đơn đặt bàn này.");
    if (id === 500 && serverErrorAttempts++ === 0) return error(500, "INTERNAL_ERROR", "Không thể tải dữ liệu. Vui lòng thử lại.");
    if (id === 500) return send(200, fixture(2));
    if (id === 10) return setTimeout(() => send(200, fixture(1)), 2000);
    if (id < 1) return error(400, "INVALID_RESERVATION_ID", "ID đơn đặt bàn phải lớn hơn 0");
    if (id > 9) return error(404, "RESERVATION_NOT_FOUND", "Không tìm thấy đơn đặt bàn");
    return send(200, fixture(id));
  }
  return error(501, "NOT_MODELLED", "Endpoint không thuộc phạm vi kiểm tra này.");
}).listen(process.env.PORT || 4901);
