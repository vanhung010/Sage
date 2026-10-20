export function readPage(value: string | null): number {
  if (!value || !/^\d+$/.test(value)) return 0;
  const number = Number(value);
  return Number.isSafeInteger(number) && number <= 1_000_000 ? number : 0;
}

export function formatDate(value: string, withTime = false): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  const options = { timeZone: "Asia/Ho_Chi_Minh" };
  const day = new Intl.DateTimeFormat("vi-VN", {
    ...options, day: "2-digit", month: "2-digit", year: "numeric",
  }).format(date);
  if (!withTime) return day;
  const time = new Intl.DateTimeFormat("vi-VN", {
    ...options, hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).format(date);
  return `${day} · ${time}`;
}

