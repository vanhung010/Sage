import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ui/Toast";
import type { CartItem } from "../types/cart";
import type { DishSummary } from "../types/menu";
import {
  MAX_QUANTITY,
  readCart,
  writeCart,
} from "../storage/cartStorage";

interface CartContextValue {
  items: CartItem[];
  totalQuantity: number;
  subtotal: number;
  addItem: (dish: DishSummary) => void;
  setQuantity: (dishId: number, quantity: number) => void;
  removeItem: (dishId: number) => void;
  clearCart: () => void;
}

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();

  const storageKey =
    user?.role === "CUSTOMER"
      ? `sage:cart:v1:${user.id}`
      : null;

  return (
    <CartStateProvider
      key={storageKey ?? "guest"}
      storageKey={storageKey}
    >
      {children}
    </CartStateProvider>
  );
}

function CartStateProvider({
  children,
  storageKey,
}: {
  children: ReactNode;
  storageKey: string | null;
}) {
  const showToast = useToast();
  // Cập nhật thông báo cùng giỏ; chỉ hiện toast sau khi React đã commit.
  // State updater giữ thuần để không phát thông báo trùng trong StrictMode.
  const [{ items, lastAddition }, setCart] = useState<{
    items: CartItem[];
    lastAddition: { name: string } | null;
  }>(() => ({ items: readCart(storageKey), lastAddition: null }));

  useEffect(() => {
    if (lastAddition) {
      showToast({
        title: "Đã thêm vào giỏ hàng",
        message: `Đã thêm “${lastAddition.name}” vào giỏ hàng của bạn.`,
      });
    }
  }, [lastAddition, showToast]);

  useEffect(() => {
    writeCart(storageKey, items);
  }, [storageKey, items]);

  function addItem(dish: DishSummary) {
    if (!storageKey || dish.status !== "AVAILABLE") return;

    setCart((state) => {
      const current = state.items;
      const existing = current.find(
        (item) => item.dishId === dish.id,
      );

      if (existing) {
        if (existing.quantity >= MAX_QUANTITY) return state;
        return {
          lastAddition: { name: dish.name },
          items: current.map((item) =>
            item.dishId === dish.id
              ? {
                  ...item,
                  name: dish.name,
                  price: dish.price,
                  imageUrl: dish.imageUrl,
                  quantity: item.quantity + 1,
                }
              : item,
          ),
        };
      }

      return {
        lastAddition: { name: dish.name },
        items: [
          ...current,
          {
            dishId: dish.id,
            name: dish.name,
            imageUrl: dish.imageUrl,
            price: dish.price,
            quantity: 1,
          },
        ],
      };
    });
  }

  function setQuantity(dishId: number, quantity: number) {
    if (
      !storageKey ||
      !Number.isInteger(quantity) ||
      quantity < 1 ||
      quantity > MAX_QUANTITY
    ) {
      return;
    }

    setCart((state) => ({
      ...state,
      items: state.items.map((item) =>
        item.dishId === dishId
          ? { ...item, quantity }
          : item,
      ),
    }));
  }

  function removeItem(dishId: number) {
    setCart((state) => ({
      ...state,
      items: state.items.filter((item) => item.dishId !== dishId),
    }));
  }

  function clearCart() {
    setCart((state) => ({ ...state, items: [] }));
    writeCart(storageKey, []);
  }

  const totalQuantity = items.reduce(
    (total, item) => total + item.quantity,
    0,
  );

  const subtotal = items.reduce(
    (total, item) => total + item.price * item.quantity,
    0,
  );

  return (
    <CartContext.Provider
      value={{
        items,
        totalQuantity,
        subtotal,
        addItem,
        setQuantity,
        removeItem,
        clearCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);

  if (!context) {
    throw new Error("useCart phải nằm trong CartProvider");
  }

  return context;
}
