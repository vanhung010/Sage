import { Badge } from "../../ui/Badge";
import { Button } from "../../ui/Button";

import "./DishCard.css";

interface DishCardProps {
  name: string;
  description: string;
  price: number;

  imageUrl?: string;

  badge?: string;

  onAdd?: () => void;
  onClick?: () => void;

  className?: string;

  addDisabled?: boolean;
}

export function DishCard({
  name,
  description,
  price,
  imageUrl,
  badge,
  onAdd,
  onClick,
  className = "",
  addDisabled = false,
}: DishCardProps) {
  const classes = [
    "sage-dish-card",
    onClick ? "sage-dish-card--clickable" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  const formattedPrice = new Intl.NumberFormat(
    "vi-VN",
    {
      style: "currency",
      currency: "VND",
    }
  ).format(price);

  function handleAdd(
    event: React.MouseEvent<HTMLButtonElement>
  ) {
    event.stopPropagation();

    onAdd?.();
  }

  return (
    <article
      className={classes}
      onClick={onClick}
    >
      <div className="sage-dish-card__image-area">
        {imageUrl && (
          <img
            className="sage-dish-card__image"
            src={imageUrl}
            alt={name}
          />
        )}

        {badge && (
          <div className="sage-dish-card__badge">
            <Badge>
              {badge}
            </Badge>
          </div>
        )}
      </div>

      <div className="sage-dish-card__content">
        <h3 className="sage-dish-card__name">
          {name}
        </h3>

        <p className="sage-dish-card__description">
          {description}
        </p>

        <div className="sage-dish-card__footer">
          <span className="sage-dish-card__price">
            {formattedPrice}
          </span>

          
           <Button
            variant="outline"
            size="md"
            disabled={addDisabled}
            onClick={handleAdd}
          >
            Thêm món
          </Button>
        </div>
      </div>
    </article>
  );
}