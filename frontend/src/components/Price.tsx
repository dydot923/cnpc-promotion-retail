import { Typography } from "antd";
import type { CSSProperties } from "react";

type PriceProps = {
  amount?: string | number | null;
  size?: "small" | "medium" | "large";
  variant?: "normal" | "promo" | "muted" | "success";
  strike?: boolean;
};

const sizeStyles: Record<NonNullable<PriceProps["size"]>, CSSProperties> = {
  small: { fontSize: 13 },
  medium: { fontSize: 18, fontWeight: 700 },
  large: { fontSize: 28, fontWeight: 800 }
};

const variantClass: Record<NonNullable<PriceProps["variant"]>, string> = {
  normal: "price-normal",
  promo: "price-promo",
  muted: "price-muted",
  success: "price-success"
};

export default function Price({ amount, size = "medium", variant = "normal", strike = false }: PriceProps) {
  const text = amount === undefined || amount === null || amount === "" ? "--" : String(amount);
  const display = text === "--" || text.startsWith("¥") ? text : `¥${text}`;

  return (
    <Typography.Text className={variantClass[variant]} delete={strike} style={sizeStyles[size]}>
      {display}
    </Typography.Text>
  );
}
