import {
  CarOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  GiftOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  SearchOutlined,
  ShopOutlined,
  ShoppingCartOutlined,
  SwapOutlined,
  TagsOutlined,
  ThunderboltOutlined,
  WarningOutlined
} from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Checkbox,
  Collapse,
  Divider,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message
} from "antd";
import type { InputRef } from "antd/es/input";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { useSearchParams } from "react-router-dom";
import { calculateCheckout, confirmCheckout, fetchExchangeOffers } from "../api/checkout";
import { fetchProductByBarcode, searchProducts } from "../api/products";
import { fetchStations } from "../api/stations";
import EmptyState from "../components/EmptyState";
import Price from "../components/Price";
import type {
  BlockedPromotion,
  Candidate,
  CartItem,
  CheckoutCalculateRequest,
  CheckoutCalculateResponse,
  CheckoutExchangeOffer,
  Coupon,
  FuelType,
  GiftCoupon,
  GiftItem,
  InventoryWarning,
  ProductCatalogItem
} from "../types";

type ProductForm = {
  productCode: string;
  barcode?: string;
  name: string;
  unitPrice: number;
  quantity: number;
  category?: string;
};

type FuelForm = {
  fuelType: FuelType;
  fuelGrade?: string;
  amount: number;
  volume: number;
};

type CustomerForm = {
  member: boolean;
  memberLevel?: string;
  memberCode?: string;
};

type TransactionContextForm = {
  stationCode?: string;
  stationType: string;
  stationProvince?: string;
  businessDate?: string;
  businessTime?: string;
  rechargeAmount?: number;
  paymentMethod?: string;
  memberBirthMonth?: number;
  couponId?: string;
  couponName?: string;
  couponFaceValue?: number;
  couponMinSpendAmount?: number;
  couponApplicableCategories?: string;
  couponExcludedCategories?: string;
  couponStackable?: boolean;
  selectedCouponIds?: string;
};

type CheckoutMode = "shop" | "fuel" | "exchange" | "coupon";

type ProductLookupResult = {
  query: string;
  products: ProductCatalogItem[];
  selected?: ProductCatalogItem;
};

type DemoCase = {
  key: string;
  label: string;
  description: string;
  expectedRuleIds: string[];
  expectedGiftOptions?: string[][];
  expectedCoupons?: { name: string; amount: number; quantity: number }[];
  expectedPointsMultiplier?: number;
  cartItems: CartItem[];
  fuel: FuelForm;
  customer: CustomerForm;
  context?: Partial<TransactionContextForm>;
};

const demoCases: DemoCase[] = [
  {
    key: "a1-500",
    label: "A1 逢7气惠-LNG满500",
    description: "7/17/27 日，LNG 消费 500 元，赠 LNG/便利店券并预览 3 倍积分。",
    expectedRuleIds: ["abv2-a1-day7-gas-coupon"],
    expectedCoupons: [
      { name: "10元LNG券", amount: 10, quantity: 1 },
      { name: "6元便利店商品券", amount: 6, quantity: 2 }
    ],
    expectedPointsMultiplier: 3,
    cartItems: [],
    fuel: fuel("LNG", "LNG", 500, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-17", stationType: "gas_filling_station" }
  },
  {
    key: "a1-1000",
    label: "A1 逢7气惠-LNG满1000",
    description: "LNG 消费 1000 元，赠 30 元 LNG 券 1 张和 12 元便利店券 1 张。",
    expectedRuleIds: ["abv2-a1-day7-gas-coupon"],
    expectedCoupons: [
      { name: "30元LNG券", amount: 30, quantity: 1 },
      { name: "12元便利店商品券", amount: 12, quantity: 1 }
    ],
    expectedPointsMultiplier: 3,
    cartItems: [],
    fuel: fuel("LNG", "LNG", 1000, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-17", stationType: "gas_filling_station" }
  },
  {
    key: "a1-1500",
    label: "A1 逢7气惠-LNG满1500",
    description: "LNG 消费 1500 元，赠 60 元 LNG 券 1 张和 12 元便利店券 2 张。",
    expectedRuleIds: ["abv2-a1-day7-gas-coupon"],
    expectedCoupons: [
      { name: "60元LNG券", amount: 60, quantity: 1 },
      { name: "12元便利店商品券", amount: 12, quantity: 2 }
    ],
    expectedPointsMultiplier: 3,
    cartItems: [],
    fuel: fuel("LNG", "LNG", 1500, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-17", stationType: "gas_filling_station" }
  },
  {
    key: "a1-2000",
    label: "A1 逢7气惠-LNG满2000",
    description: "LNG 消费 2000 元，赠 100 元 LNG 券 1 张和 12 元便利店券 3 张。",
    expectedRuleIds: ["abv2-a1-day7-gas-coupon"],
    expectedCoupons: [
      { name: "100元LNG券", amount: 100, quantity: 1 },
      { name: "12元便利店商品券", amount: 12, quantity: 3 }
    ],
    expectedPointsMultiplier: 3,
    cartItems: [],
    fuel: fuel("LNG", "LNG", 2000, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-17", stationType: "gas_filling_station" }
  },
  {
    key: "a3",
    label: "A3 加气站便利店9折",
    description: "加气站非逢 7 日期，便利店商品按 9 折结算。",
    expectedRuleIds: ["abv2-a3-gas-filling-discount"],
    cartItems: [item("70727893", "优斯麦尔 西梅复合果汁饮品 0.3L", 2, 5.5, "包装饮料", 30)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16", stationType: "gas_filling_station" }
  },
  {
    key: "a4",
    label: "A4 逢8 CN98每升立减",
    description: "8/18/28 日，CN98 加油 10 升，每升立减 0.8 元。",
    expectedRuleIds: ["abv2-a4-cn98-volume-discount"],
    cartItems: [],
    fuel: fuel("CN98", "98", 100, 10),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-18" }
  },
  {
    key: "a5-1000-gold",
    label: "A5 超级十惠-充值1000金卡",
    description: "10/20/30 日，金卡会员充值 1000 元，展示普通券包与金卡加赠券。",
    expectedRuleIds: ["abv2-a5-day10-super-1000-gold"],
    expectedCoupons: [
      { name: "A5 Day10 12 yuan gasoline coupon", amount: 12, quantity: 2 },
      { name: "A5 Day10 12 yuan convenience store coupon", amount: 12, quantity: 3 },
      { name: "A5 Day10 10 yuan car wash coupon", amount: 10, quantity: 3 },
      { name: "A5 Day10 15 yuan high-grade gasoline coupon", amount: 15, quantity: 1 }
    ],
    cartItems: [],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-20", rechargeAmount: 1000 }
  },
  {
    key: "a5-1000-normal",
    label: "A5 超级十惠-充值1000普通",
    description: "非金卡会员充值 1000 元，赠汽油券 2 张、便利店券 3 张、洗车券 3 张。",
    expectedRuleIds: ["abv2-a5-day10-super-1000-normal"],
    expectedCoupons: [
      { name: "A5 Day10 12 yuan gasoline coupon", amount: 12, quantity: 2 },
      { name: "A5 Day10 12 yuan convenience store coupon", amount: 12, quantity: 3 },
      { name: "A5 Day10 10 yuan car wash coupon", amount: 10, quantity: 3 }
    ],
    cartItems: [],
    fuel: fuel(),
    customer: { member: true, memberLevel: "silver", memberCode: "member-002" },
    context: { businessDate: "2026-07-20", rechargeAmount: 1000 }
  },
  {
    key: "a5-2000-gold",
    label: "A5 超级十惠-充值2000金卡",
    description: "金卡会员充值 2000 元，赠普通券包并加赠 2 张高标号汽油券。",
    expectedRuleIds: ["abv2-a5-day10-super-2000-gold"],
    expectedCoupons: [
      { name: "A5 Day10 12 yuan gasoline coupon", amount: 12, quantity: 5 },
      { name: "A5 Day10 12 yuan convenience store coupon", amount: 12, quantity: 6 },
      { name: "A5 Day10 10 yuan car wash coupon", amount: 10, quantity: 6 },
      { name: "A5 Day10 15 yuan high-grade gasoline coupon", amount: 15, quantity: 2 }
    ],
    cartItems: [],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-20", rechargeAmount: 2000 }
  },
  {
    key: "a5-2000-normal",
    label: "A5 超级十惠-充值2000普通",
    description: "非金卡会员充值 2000 元，赠汽油券 5 张、便利店券 6 张、洗车券 6 张。",
    expectedRuleIds: ["abv2-a5-day10-super-2000-normal"],
    expectedCoupons: [
      { name: "A5 Day10 12 yuan gasoline coupon", amount: 12, quantity: 5 },
      { name: "A5 Day10 12 yuan convenience store coupon", amount: 12, quantity: 6 },
      { name: "A5 Day10 10 yuan car wash coupon", amount: 10, quantity: 6 }
    ],
    cartItems: [],
    fuel: fuel(),
    customer: { member: true, memberLevel: "silver", memberCode: "member-002" },
    context: { businessDate: "2026-07-20", rechargeAmount: 2000 }
  },
  {
    key: "a6",
    label: "A6 小额充值666赠券",
    description: "非十惠日充值 666 元，赠 3 张汽油券和 3 张便利店券。",
    expectedRuleIds: ["abv2-a6-small-recharge-666"],
    expectedCoupons: [
      { name: "Small recharge 10 yuan gasoline coupon", amount: 10, quantity: 3 },
      { name: "Small recharge 12 yuan store coupon", amount: 12, quantity: 3 }
    ],
    cartItems: [],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-21", rechargeAmount: 666 }
  },
  {
    key: "e1",
    label: "E1 买油赠非油券",
    description: "会员汽油消费满 230 元，赠香烟券和便利店券。",
    expectedRuleIds: ["abv2-e1-gasoline-gift-coupons"],
    expectedCoupons: [
      { name: "15元香烟券", amount: 15, quantity: 1 },
      { name: "6元便利店商品券", amount: 6, quantity: 1 }
    ],
    cartItems: [],
    fuel: fuel("GASOLINE", "92", 230, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-11" }
  },
  {
    key: "e1-diesel",
    label: "E1 柴油满280赠非油券",
    description: "会员柴油消费满 280 元，赠 15 元香烟券和 6 元便利店券。",
    expectedRuleIds: ["abv2-e1-diesel-gift-coupons"],
    expectedCoupons: [
      { name: "15元香烟券", amount: 15, quantity: 1 },
      { name: "6元便利店商品券", amount: 6, quantity: 1 }
    ],
    cartItems: [],
    fuel: fuel("DIESEL", "0", 280, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-11" }
  },
  {
    key: "e2",
    label: "E2 伊力特整件赠汽油券",
    description: "会员购买整件伊力特 250ml，赠 2 张 100 元汽油券。",
    expectedRuleIds: ["abv2-e2-ilite-250-case-coupon"],
    expectedCoupons: [{ name: "100元汽油券", amount: 100, quantity: 2 }],
    cartItems: [item("70690981", "优斯麦尔 46度伊力特（佳藏）250ML", 10, 68, "酒类", 110)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-11" }
  },
  {
    key: "e2-ilite-500-jia",
    label: "E2 伊力特佳藏500ML整件赠油券",
    description: "会员购买佳藏 500ML 整件 6 瓶，赠 2 张 100 元汽油券。",
    expectedRuleIds: ["abv2-e2-ilite-500-jia-case-coupon"],
    expectedCoupons: [{ name: "100元汽油券", amount: 100, quantity: 2 }],
    cartItems: [item("70690872", "优斯麦尔 46度伊力特（佳藏）500ML", 6, 118, "酒类", 110)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-11" }
  },
  {
    key: "e2-ilite-500-li",
    label: "E2 伊力特礼藏500ML整件赠油券",
    description: "会员购买礼藏 500ML 整件 6 瓶，赠 4 张 100 元汽油券。",
    expectedRuleIds: ["abv2-e2-ilite-500-li-case-coupon"],
    expectedCoupons: [{ name: "100元汽油券", amount: 100, quantity: 4 }],
    cartItems: [item("70690982", "优斯麦尔 52度伊力特（礼藏）500ML", 6, 298, "酒类", 110)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-11" }
  },
  {
    key: "e2-wing-card",
    label: "E2 翼卡通399元购卡赠油券",
    description: "会员 399 元购买新疆旅游翼卡通，赠 2 张 100 元汽油券（满 101 元可用）。",
    expectedRuleIds: ["abv2-e2-wing-card-399-coupon"],
    expectedCoupons: [{ name: "100元汽油券", amount: 100, quantity: 2 }],
    cartItems: [item("demo-wing-card", "新疆旅游翼卡通", 1, 399, "日用品", 30)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-11" }
  },
  {
    key: "f1",
    label: "F1 CNG买气送水",
    description: "CNG 单笔满 50 元，赠 2 瓶格桑泉。",
    expectedRuleIds: ["abv2-f1-cng-gift-water"],
    expectedGiftOptions: [["70545526"]],
    cartItems: [],
    fuel: fuel("CNG", "CNG", 50, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-11", stationType: "gas_filling_station" }
  },
  {
    key: "f1-lng",
    label: "F1 LNG满1000赠4瓶水",
    description: "LNG 单笔满 1000 元，赠 4 瓶格桑泉 500ML 矿泉水。",
    expectedRuleIds: ["abv2-f1-lng-gift-water"],
    expectedGiftOptions: [["70545526"]],
    cartItems: [],
    fuel: fuel("LNG", "LNG", 1000, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-11", stationType: "gas_filling_station" }
  },
  {
    key: "g1",
    label: "G1 逢7加气站全场9折",
    description: "7/17/27 日，加气站便利店商品全场 9 折。",
    expectedRuleIds: ["abv2-g1-day7-gas-filling-discount"],
    cartItems: [item("70727893", "优斯麦尔 西梅复合果汁饮品 0.3L", 2, 5.5, "包装饮料", 30)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-17", stationType: "gas_filling_station" }
  },
  {
    key: "g2",
    label: "G2 逢9油站9折+3倍积分",
    description: "9/19/29 日，加油站便利店 9 折并预览 3 倍积分。",
    expectedRuleIds: ["abv2-g2-day9-gas-station-discount"],
    cartItems: [item("70727893", "优斯麦尔 西梅复合果汁饮品 0.3L", 2, 5.5, "包装饮料", 30)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-19" }
  },
  {
    key: "g3",
    label: "G3 9.9元零食专区",
    description: "录入活动看板 9.9 专区真实商品，执行固定价 9.9 元。",
    expectedRuleIds: ["abv2-99-zone-70000639"],
    cartItems: [item("70000639", "好丽友 蛋黄派 6枚", 1, 11, "零食", 6)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "g4",
    label: "G4 赛事啤酒赠券+夜间折扣",
    description: "赛事期 19:00，会员购买啤酒满 66 元，赠券并叠加夜间 8.8 折。",
    expectedRuleIds: ["abv2-g4-event-beer-coupon", "abv2-g4-event-night-discount"],
    cartItems: [item("70488371", "乌苏 小麦白罐装啤酒 500ML", 9, 8, "啤酒", 30)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-15", businessTime: "19:00:00" }
  },
  {
    key: "g5",
    label: "G5 中秋满减+赠券",
    description: "9 月会员购买月饼礼盒满 226 元，减 50 元并赠 2 张汽油券。",
    expectedRuleIds: ["abv2-g5-mid-autumn-composite"],
    cartItems: [item("70538246", "葡萄树 乳酪月饼 450G", 2, 169, "月饼礼盒", 30)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-09-15" }
  },
  {
    key: "g6-ilite-250",
    label: "G6 伊力特250ML会员价+赠券",
    description: "会员购买 2 瓶伊力特 250ml，显示会员固定价和便利店券。",
    expectedRuleIds: ["abv2-g6-ilite-250-fixed", "abv2-g6-ilite-250-coupon"],
    expectedCoupons: [{ name: "12元商品券", amount: 12, quantity: 1 }],
    cartItems: [item("70690981", "优斯麦尔 46度伊力特（佳藏）250ML", 2, 68, "酒类", 110)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "g6-ilite-500-jia",
    label: "G6 伊力特佳藏500ML会员价+赠券",
    description: "会员购买 2 瓶佳藏 500ML，执行 216 元会员价并赠 1 张 12 元券。",
    expectedRuleIds: ["abv2-g6-ilite-500-jia-fixed", "abv2-g6-ilite-500-jia-coupon"],
    expectedCoupons: [{ name: "12元商品券", amount: 12, quantity: 1 }],
    cartItems: [item("70690872", "优斯麦尔 46度伊力特（佳藏）500ML", 2, 128, "酒类", 110)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "g6-ilite-500-li",
    label: "G6 伊力特礼藏500ML会员价+赠券",
    description: "会员购买 2 瓶礼藏 500ML，执行 516 元会员价并赠 2 张 12 元券。",
    expectedRuleIds: ["abv2-g6-ilite-500-li-fixed", "abv2-g6-ilite-500-li-coupon"],
    expectedCoupons: [{ name: "12元商品券", amount: 12, quantity: 2 }],
    cartItems: [item("70690982", "优斯麦尔 52度伊力特（礼藏）500ML", 2, 298, "酒类", 110)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "g6-cigarette-200",
    label: "G6 香烟满200赠品二选一",
    description: "香烟消费 224 元，可选择 2 瓶优斯麦尔果汁或 2 个自有奶。",
    expectedRuleIds: ["abv2-g6-cigarette-200-gift-choice"],
    expectedGiftOptions: [["70727875"], ["70559364"]],
    cartItems: [item("70030041", "黄山 金皖硬盒香烟(包) 13MG", 8, 28, "香烟", 50)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "g6-cigarette-555",
    label: "G6 香烟满555赠伊力特250ML",
    description: "香烟消费 560 元，赠 1 瓶优斯麦尔 46 度伊力特（佳藏）250ML。",
    expectedRuleIds: ["abv2-g6-cigarette-555-gift-ilite250"],
    expectedGiftOptions: [["70690981"]],
    cartItems: [item("70030041", "黄山 金皖硬盒香烟(包) 13MG", 20, 28, "香烟", 50)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "g6-cigarette-888",
    label: "G6 香烟满888赠伊力特500ML",
    description: "香烟消费 896 元，赠 1 瓶优斯麦尔 46 度伊力特（佳藏）500ML。",
    expectedRuleIds: ["abv2-g6-cigarette-888-gift-ilite500"],
    expectedGiftOptions: [["70690872"]],
    cartItems: [item("70030041", "黄山 金皖硬盒香烟(包) 13MG", 32, 28, "香烟", 50)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "g6-store-gift",
    label: "G6 便利店满额赠品二选一",
    description: "便利店商品消费 44 元，可选择 1 盒英九庄园茶叶或 1 个自有奶。",
    expectedRuleIds: ["abv2-g6-store-36-gift-choice"],
    expectedGiftOptions: [["demo-yingjiu-tea"], ["70559364"]],
    cartItems: [item("70727893", "优斯麦尔 西梅复合果汁饮品 0.3L", 8, 5.5, "包装饮料", 30)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "g6-cotton-film",
    label: "G6 棉包膜9卷赠完整礼包",
    description: "购买棉包膜 9 卷，赠 2 件伊力特、1 件 M 枕牛奶、4 件红牛和 100 副手套。",
    expectedRuleIds: ["abv2-g6-cotton-film-9-gift-pack"],
    expectedGiftOptions: [["70690981", "70559368", "70356177", "70657932"]],
    cartItems: [item("demo-cotton-film", "棉花膜（活动看板商品）", 9, 2000, "化工农资", 30)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "g7",
    label: "G7 单品安全促销价",
    description: "录入原缺价清单商品，执行已确认的安全促销价 4.25 元。",
    expectedRuleIds: ["audit-personalized-fixed-70485561"],
    cartItems: [item("70485561", "果子熟了 金桂乌龙茶 500ML", 1, 5, "包装饮料", 7)],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "h1",
    label: "H1 加油换购驾驶包",
    description: "汽油满 200 元，驾驶包商品按组合价 25 元换购。",
    expectedRuleIds: ["abv2-bundle-abv2-driving-package"],
    cartItems: [
      item("70341453", "好客壹生牌 软抽 3层120抽", 1, 15, "日用品", 343),
      item("70356177", "红牛 维生素风味饮料 250ML", 3, 6, "包装饮料", 5747),
      item("70536790", "咔咔酷优特 0℃玻璃水 2L", 2, 8, "车辅", 1264)
    ],
    fuel: fuel("GASOLINE", "92", 220, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  },
  {
    key: "h2",
    label: "H2 加油换购红牛3罐",
    description: "汽油满 180 元，3 罐红牛按 12 元换购。",
    expectedRuleIds: ["abv2-h2-redbull-gasoline"],
    cartItems: [item("70356177", "红牛 维生素风味饮料 250ML", 3, 6, "包装饮料", 5747)],
    fuel: fuel("GASOLINE", "92", 180, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "demo-member-002" },
    context: { businessDate: "2026-07-16" }
  }
];

const demoCaseOptions = [
  {
    label: "品牌日与充值活动",
    options: demoCases
      .filter((demo) => demo.key.startsWith("a") || demo.key === "g1" || demo.key === "g2")
      .map((demo) => ({ value: demo.key, label: demo.label }))
  },
  {
    label: "油非与气非互动",
    options: demoCases
      .filter((demo) => demo.key.startsWith("e") || demo.key.startsWith("f"))
      .map((demo) => ({ value: demo.key, label: demo.label }))
  },
  {
    label: "便利店与纯非促销",
    options: demoCases
      .filter((demo) => demo.key.startsWith("g") && !["g1", "g2"].includes(demo.key))
      .map((demo) => ({ value: demo.key, label: demo.label }))
  },
  {
    label: "加油换购",
    options: demoCases
      .filter((demo) => demo.key.startsWith("h"))
      .map((demo) => ({ value: demo.key, label: demo.label }))
  }
];

const stationTypeOptions = [
  { value: "gas_station", label: "加油站" },
  { value: "gas_filling_station", label: "加气站（CNG/LNG）" }
];

const fuelTypeOptions = [
  { value: "NONE", label: "无油品" },
  { value: "GASOLINE", label: "汽油" },
  { value: "DIESEL", label: "柴油" },
  { value: "CNG", label: "CNG" },
  { value: "LNG", label: "LNG" },
  { value: "CN98", label: "CN98" }
];

const memberLevelOptions = [
  { value: "gold", label: "金卡" },
  { value: "silver", label: "银卡" },
  { value: "normal", label: "普通" }
];

export default function CheckoutPage() {
  const [searchParams] = useSearchParams();
  const requestedDemoKey = searchParams.get("demo") || undefined;
  const [productForm] = Form.useForm<ProductForm>();
  const [fuelForm] = Form.useForm<FuelForm>();
  const [customerForm] = Form.useForm<CustomerForm>();
  const [contextForm] = Form.useForm<TransactionContextForm>();
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [barcode, setBarcode] = useState("");
  const [mode, setMode] = useState<CheckoutMode>("shop");
  const [loadedDemoKey, setLoadedDemoKey] = useState<string>();
  const [selectedCandidateId, setSelectedCandidateId] = useState<string>();
  const [latestConfirmationId, setLatestConfirmationId] = useState<string>();
  const [api, contextHolder] = message.useMessage();
  const barcodeInputRef = useRef<InputRef>(null);
  const watchedFuelType = Form.useWatch("fuelType", fuelForm) as FuelType | undefined;
  const watchedFuelAmount = Form.useWatch("amount", fuelForm) as number | undefined;
  const watchedStationType = Form.useWatch("stationType", contextForm) as string | undefined;
  const watchedStationProvince = Form.useWatch("stationProvince", contextForm) as string | undefined;
  const watchedBusinessDate = Form.useWatch("businessDate", contextForm) as string | undefined;
  const watchedRechargeAmount = Form.useWatch("rechargeAmount", contextForm) as number | undefined;

  const stationsQuery = useQuery({ queryKey: ["stations"], queryFn: fetchStations, staleTime: 5 * 60_000 });
  const stationOptions = useMemo(
    () =>
      (stationsQuery.data || []).map((station) => ({
        value: station.stationCode,
        label: `${station.stationName}（${station.stationCode}）`
      })),
    [stationsQuery.data]
  );

  const checkoutMutation = useMutation({
    mutationFn: async (request: CheckoutCalculateRequest) => {
      const startedAt = performance.now();
      const response = await calculateCheckout(request);
      const elapsed = Math.max(1, Math.round(performance.now() - startedAt));
      localStorage.setItem("lastCalculationMs", String(elapsed));
      window.dispatchEvent(new Event("checkout-calculation-finished"));
      return response;
    }
  });

  const confirmMutation = useMutation({
    mutationFn: confirmCheckout,
    onSuccess: (confirmation) => {
      setLatestConfirmationId(confirmation.confirmationId);
      api.success(`结算已确认：${confirmation.confirmationId}`);
    }
  });

  const productLookupMutation = useMutation<ProductLookupResult, Error, string>({
    mutationFn: async (query) => {
      if (looksLikeBarcode(query)) {
        try {
          const product = await fetchProductByBarcode(query);
          return { query, products: [product], selected: product };
        } catch {
          // 商品编码也可能是纯数字，条码未命中时继续执行模糊搜索。
        }
      }

      const products = await searchProducts(query);
      const exactMatch = products.find((product) => product.productCode === query || product.barcode === query);
      return {
        query,
        products,
        selected: exactMatch || (products.length === 1 ? products[0] : undefined)
      };
    },
    onSuccess: ({ products, selected }) => {
      if (selected) {
        selectProduct(selected);
        return;
      }
      if (products.length === 0) {
        api.warning("没有找到商品，请换一个名称、编码或条码");
        barcodeInputRef.current?.focus();
        return;
      }
      api.info(`找到 ${products.length} 个商品，请选择`);
    }
  });

  const exchangeOffersQuery = useQuery({
    queryKey: [
      "checkout-exchange-offers",
      watchedFuelType,
      watchedFuelAmount,
      watchedStationType,
      watchedStationProvince,
      watchedBusinessDate
    ],
    queryFn: () =>
      fetchExchangeOffers({
        fuelType: watchedFuelType || "GASOLINE",
        fuelAmount: Number(watchedFuelAmount || 0),
        businessDate: watchedBusinessDate || currentBusinessTime().businessDate,
        stationType: watchedStationType || "gas_station",
        stationProvince: watchedStationProvince || "新疆"
      }),
    enabled: mode === "exchange" && Boolean(watchedFuelType && watchedFuelType !== "NONE"),
    staleTime: 10_000
  });

  const result = checkoutMutation.data;
  const productResults = useMemo(
    () => sortProductResults(productLookupMutation.data?.products || [], productLookupMutation.data?.query || ""),
    [productLookupMutation.data]
  );
  const activeDemo = useMemo(() => demoCases.find((demo) => demo.key === loadedDemoKey), [loadedDemoKey]);
  const promotionCandidates = useMemo(
    () => uniquePromotionCandidates(result?.availableCandidates || []),
    [result]
  );
  const backendRecommended = useMemo(
    () => result?.availableCandidates.find((candidate) => candidate.candidateId === result.recommendedCandidateId),
    [result]
  );
  const acceptanceCandidate = useMemo(
    () => activeDemo
      ? result?.availableCandidates.find((candidate) => activeDemo.expectedRuleIds.includes(candidate.ruleId))
      : undefined,
    [activeDemo, result]
  );
  const recommended = acceptanceCandidate || backendRecommended;
  const selectedCandidate = useMemo(() => {
    if (!result) {
      return undefined;
    }
    return (
      result.availableCandidates.find((candidate) => candidate.candidateId === selectedCandidateId) ||
      (selectedCandidateId === result.originalPriceFallback.candidateId ? result.originalPriceFallback : undefined)
    );
  }, [result, selectedCandidateId]);
  const cartCount = useMemo(() => cartItems.reduce((total, cartItem) => total + Number(cartItem.quantity || 0), 0), [cartItems]);
  const cartTotal = useMemo(() => sumCart(cartItems), [cartItems]);
  const hasTransactionInput = cartItems.length > 0
    || Number(watchedFuelAmount || 0) > 0
    || Number(watchedRechargeAmount || 0) > 0;
  const transactionInputTotal = cartTotal
    + Number(watchedFuelAmount || 0)
    + Number(watchedRechargeAmount || 0);

  useEffect(() => {
    barcodeInputRef.current?.focus();
  }, []);

  useEffect(() => {
    if (!requestedDemoKey || requestedDemoKey === loadedDemoKey) {
      return;
    }
    const requestedDemo = demoCases.find((demo) => demo.key === requestedDemoKey);
    if (requestedDemo) {
      loadDemo(requestedDemo);
    }
  }, [loadedDemoKey, requestedDemoKey]);

  useEffect(() => {
    if (result) {
      setSelectedCandidateId(recommended?.candidateId || result.originalPriceFallback.candidateId);
      setLatestConfirmationId(undefined);
      confirmMutation.reset();
    }
  }, [recommended?.candidateId, result]);

  const cartColumns: ColumnsType<CartItem> = [
    {
      title: "商品",
      dataIndex: "name",
      width: 240,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.name}</Typography.Text>
          <Typography.Text type="secondary">
            {record.productCode}
            {record.category ? ` / ${record.category}` : ""}
          </Typography.Text>
        </Space>
      )
    },
    { title: "条码", dataIndex: "barcode", width: 140, render: (value) => value || "-" },
    { title: "单价", dataIndex: "unitPrice", width: 96, render: (value) => <Price amount={value} size="small" /> },
    {
      title: "数量",
      dataIndex: "quantity",
      width: 126,
      render: (_, record) => (
        <InputNumber
          min={1}
          precision={0}
          value={record.quantity}
          onChange={(value) => updateQuantity(record.lineId, Number(value || 1))}
        />
      )
    },
    { title: "小计", width: 100, render: (_, record) => <Price amount={lineTotal(record)} size="small" /> },
    {
      title: "",
      width: 54,
      render: (_, record) => (
        <Button aria-label="删除商品" icon={<DeleteOutlined />} onClick={() => removeItem(record.lineId)} />
      )
    }
  ];

  function resetCalculation() {
    checkoutMutation.reset();
    confirmMutation.reset();
    setSelectedCandidateId(undefined);
    setLatestConfirmationId(undefined);
  }

  function loadDemo(demo: DemoCase) {
    setLoadedDemoKey(demo.key);
    setCartItems(demo.cartItems);
    fuelForm.setFieldsValue(demo.fuel);
    customerForm.setFieldsValue(demo.customer);
    contextForm.setFieldsValue({
      ...defaultContext(),
      memberBirthMonth: demo.customer.member ? 7 : undefined,
      ...(demo.context || {})
    });
    setMode(demo.key.startsWith("h") ? "exchange" : demo.fuel.fuelType === "NONE" ? "shop" : "fuel");
    resetCalculation();
    barcodeInputRef.current?.focus();
  }

  function applyMode(nextMode: CheckoutMode) {
    setLoadedDemoKey(undefined);
    setMode(nextMode);
    if ((nextMode === "fuel" || nextMode === "exchange") && fuelForm.getFieldValue("fuelType") === "NONE") {
      fuelForm.setFieldsValue(fuel("GASOLINE", "92", 0, 0));
    }
    if (nextMode === "coupon") {
      customerForm.setFieldsValue({ member: true, memberLevel: "gold", memberCode: "demo-member-002" });
    }
    resetCalculation();
    barcodeInputRef.current?.focus();
  }

  function queryProduct() {
    const value = barcode.trim();
    if (!value) {
      api.warning("请输入商品名称、商品编码或条码");
      barcodeInputRef.current?.focus();
      return;
    }
    productLookupMutation.mutate(value);
  }

  function fillProductForm(product: ProductCatalogItem) {
    productForm.setFieldsValue({
      productCode: product.productCode,
      barcode: product.barcode || "",
      name: product.productName,
      unitPrice: Number(product.unitPrice || 0),
      quantity: 1,
      category: product.category || ""
    });
  }

  function addOrIncrementProduct(product: ProductCatalogItem) {
    resetCalculation();
    setCartItems((items) => {
      const existing = items.find((cartItem) => cartItem.productCode === product.productCode);
      if (existing) {
        return items.map((cartItem) =>
          cartItem.productCode === product.productCode ? { ...cartItem, quantity: cartItem.quantity + 1 } : cartItem
        );
      }
      return [
        ...items,
        {
          lineId: `line-${product.productCode}-${Date.now()}`,
          productCode: product.productCode,
          barcode: product.barcode || null,
          name: product.productName,
          quantity: 1,
          unitPrice: Number(product.unitPrice || 0),
          category: product.category || null,
          inventoryQuantity: Number(product.inventoryQuantity || 0)
        }
      ];
    });
  }

  function selectProduct(product: ProductCatalogItem) {
    fillProductForm(product);
    addOrIncrementProduct(product);
    setBarcode("");
    productLookupMutation.reset();
    api.success(`已加入：${product.productName}`);
    barcodeInputRef.current?.focus();
  }

  function addExchangeOffer(offer: CheckoutExchangeOffer) {
    if (!offer.eligible) {
      api.warning(offer.blockedReasons[0] || "当前换购条件未满足");
      return;
    }
    resetCalculation();
    setCartItems((items) => {
      const existing = items.find((cartItem) => cartItem.productCode === offer.productCode);
      if (existing) {
        return items.map((cartItem) =>
          cartItem.productCode === offer.productCode
            ? { ...cartItem, quantity: Math.max(cartItem.quantity, offer.exchangeQuantity) }
            : cartItem
        );
      }
      return [
        ...items,
        {
          lineId: `line-${offer.ruleId}-${offer.productCode}-${Date.now()}`,
          productCode: offer.productCode,
          barcode: offer.barcode || null,
          name: offer.productName,
          quantity: offer.exchangeQuantity,
          unitPrice: Number(offer.unitPrice || 0),
          category: offer.category || null,
          inventoryQuantity: Number(offer.inventoryQuantity || 0)
        }
      ];
    });
    api.success(`已加入换购商品：${offer.productName} x ${offer.exchangeQuantity}`);
  }

  function addItem(values: ProductForm) {
    resetCalculation();
    setCartItems((items) => [
      ...items,
      {
        lineId: `line-${Date.now()}`,
        productCode: values.productCode,
        barcode: values.barcode || null,
        name: values.name,
        quantity: values.quantity,
        unitPrice: values.unitPrice,
        category: values.category || null,
        inventoryQuantity: 0
      }
    ]);
    productForm.resetFields();
    productForm.setFieldsValue({ quantity: 1 });
    barcodeInputRef.current?.focus();
  }

  function removeItem(lineId: string) {
    resetCalculation();
    setCartItems((items) => items.filter((cartItem) => cartItem.lineId !== lineId));
  }

  function updateQuantity(lineId: string, quantity: number) {
    resetCalculation();
    setCartItems((items) =>
      items.map((cartItem) => (cartItem.lineId === lineId ? { ...cartItem, quantity: Math.max(1, quantity) } : cartItem))
    );
  }

  async function calculate() {
    const fuelValues = await fuelForm.validateFields();
    const customerValues = await customerForm.validateFields();
    const contextValues = await contextForm.validateFields();
    if (cartItems.length === 0 && Number(fuelValues.amount || 0) <= 0 && Number(contextValues.rechargeAmount || 0) <= 0) {
      api.warning("请先录入商品、油品金额或充值金额");
      barcodeInputRef.current?.focus();
      return;
    }
    const current = currentBusinessTime();
    const businessDate = contextValues.businessDate || current.businessDate;
    const businessTime = normalizeBusinessTime(contextValues.businessTime || current.businessTime);
    const selectedCouponIds = resolveSelectedCouponIds(contextValues);
    const availableCoupons = buildAvailableCoupons(contextValues);
    const request: CheckoutCalculateRequest = {
      orderContext: {
        station: {
          stationId: contextValues.stationCode || "1-A6501-C001-S001",
          stationType: contextValues.stationType || "gas_station",
          region: contextValues.stationProvince || "新疆"
        },
        customer: {
          member: customerValues.member,
          memberLevel: customerValues.member ? customerValues.memberLevel || "gold" : null,
          availableCouponIds: selectedCouponIds,
          memberBirthMonth: customerValues.member ? contextValues.memberBirthMonth || null : null,
          memberCode: customerValues.member ? customerValues.memberCode || null : null
        },
        fuel: {
          fuelType: fuelValues.fuelType,
          fuelGrade: fuelValues.fuelGrade || null,
          amount: fuelValues.amount || 0,
          volume: fuelValues.volume || 0
        },
        cartItems,
        businessDate,
        businessTime,
        availableCoupons,
        rechargeAmount: Number(contextValues.rechargeAmount || 0)
      },
      transactionDate: businessDate,
      transactionTime: businessTime,
      stationType: contextValues.stationType || "gas_station",
      stationProvince: contextValues.stationProvince || "新疆",
      stationCode: contextValues.stationCode || "1-A6501-C001-S001",
      isMember: customerValues.member,
      memberLevel: customerValues.member ? customerValues.memberLevel || "gold" : null,
      memberCode: customerValues.member ? customerValues.memberCode || null : null,
      memberBirthMonth: customerValues.member ? contextValues.memberBirthMonth || null : null,
      paymentMethod: contextValues.paymentMethod || "E_ENJOY_CARD",
      fuelType: fuelValues.fuelType,
      fuelAmount: fuelValues.amount || 0,
      fuelVolume: fuelValues.volume || 0,
      rechargeAmount: Number(contextValues.rechargeAmount || 0),
      availableCoupons,
      selectedCouponIds
    };
    setLatestConfirmationId(undefined);
    setSelectedCandidateId(undefined);
    confirmMutation.reset();
    checkoutMutation.mutate(request);
  }

  function confirmSettlement() {
    if (!result) {
      return;
    }
    const candidateId = selectedCandidateId || result.originalPriceFallback.candidateId;
    confirmMutation.mutate({
      orderNo: `order-${Date.now()}`,
      calculationId: result.calculationId,
      selectedCandidateId: candidateId,
      skippedPromotion: candidateId === result.originalPriceFallback.candidateId,
      operatorId: "cashier-001",
      operatorName: "收银员"
    });
  }

  function startNewCheckout() {
    setCartItems([]);
    setMode("shop");
    fuelForm.setFieldsValue(fuel());
    customerForm.setFieldsValue({ member: true, memberLevel: "gold", memberCode: "demo-member-002" });
    contextForm.setFieldsValue(defaultContext());
    resetCalculation();
    setLoadedDemoKey(undefined);
    barcodeInputRef.current?.focus();
  }

  return (
    <>
      {contextHolder}
      <div className="page-header">
        <div>
          <Typography.Title level={1}>收银台</Typography.Title>
          <Typography.Text className="page-subtitle">扫码、核对促销、确认收款</Typography.Text>
        </div>
        <Space>
          {result?.ruleVersion ? <Tag color="blue">规则版本 {result.ruleVersion}</Tag> : null}
          {latestConfirmationId ? <Tag color="green">已确认</Tag> : <Tag>待确认</Tag>}
        </Space>
      </div>

      <div className="operator-strip">
        <StepPill index={1} title="扫码" active={cartItems.length === 0} done={cartItems.length > 0} />
        <StepPill index={2} title="核对促销" active={cartItems.length > 0 && !result} done={Boolean(result)} />
        <StepPill index={3} title="确认收款" active={Boolean(result) && !latestConfirmationId} done={Boolean(latestConfirmationId)} />
      </div>

      <div className="checkout-grid">
        <div className="checkout-column">
          <section className="panel scan-panel">
            <Space className="panel-toolbar" align="center">
              <div>
                <Typography.Title level={3} className="section-title">
                  商品录入
                </Typography.Title>
                <Typography.Text type="secondary">当前 {cartCount} 件，合计 <Price amount={cartTotal} size="small" /></Typography.Text>
              </div>
              <Space>
                <Button icon={<ReloadOutlined />} onClick={startNewCheckout}>
                  新订单
                </Button>
                <Button
                  size="large"
                  type="primary"
                  icon={<ThunderboltOutlined />}
                  loading={checkoutMutation.isPending}
                  disabled={!hasTransactionInput}
                  onClick={calculate}
                >
                  一键计算促销
                </Button>
              </Space>
            </Space>

            <Space.Compact className="full-width barcode-row">
              <Input
                ref={barcodeInputRef}
                size="large"
                value={barcode}
                placeholder="扫描条码，或输入商品名称、商品编码"
                onChange={(event) => {
                  setBarcode(event.target.value);
                  productLookupMutation.reset();
                }}
                onPressEnter={queryProduct}
                status={productLookupMutation.error ? "error" : undefined}
              />
              <Button
                size="large"
                className="blue-button"
                icon={<SearchOutlined />}
                loading={productLookupMutation.isPending}
                onClick={queryProduct}
              >
                查询并加入
              </Button>
            </Space.Compact>

            {productLookupMutation.error ? (
              <Alert
                type="error"
                showIcon
                message="商品查询失败"
                description={productLookupMutation.error.message}
                className="result-alert"
              />
            ) : null}

            {productResults.length > 0 && !productLookupMutation.data?.selected ? (
              <>
                <div className="product-result-summary">
                  <span>找到 {productResults.length} 个商品，已全部显示</span>
                  <span>有库存商品优先，可滚动查看全部结果</span>
                </div>
                <div className="product-result-list">
                  {productResults.map((product) => (
                    <button key={product.productCode} className="product-result-button" type="button" onClick={() => selectProduct(product)}>
                      <span className="product-result-info">
                        <strong>{product.productName}</strong>
                        <small>
                          编码 {product.productCode}
                          {product.barcode ? ` / 条码 ${product.barcode}` : ""}
                        </small>
                      </span>
                      <span className="product-result-side">
                        <Price amount={product.unitPrice} size="small" />
                        <small>{product.category || "未分类"} / 库存 {Number(product.inventoryQuantity || 0)}</small>
                      </span>
                    </button>
                  ))}
                </div>
              </>
            ) : null}

            <div className="mode-actions">
              <Button icon={<ShopOutlined />} className={mode === "shop" ? "mode-button active" : "mode-button"} onClick={() => applyMode("shop")}>
                便利店商品
              </Button>
              <Button icon={<CarOutlined />} className={mode === "fuel" ? "mode-button active" : "mode-button"} onClick={() => applyMode("fuel")}>
                加油站
              </Button>
              <Button icon={<SwapOutlined />} className={mode === "exchange" ? "mode-button active" : "mode-button"} onClick={() => applyMode("exchange")}>
                加油换购
              </Button>
              <Button icon={<TagsOutlined />} className={mode === "coupon" ? "mode-button active" : "mode-button"} onClick={() => applyMode("coupon")}>
                用券结算
              </Button>
            </div>

            <ModeContext mode={mode} />

            {mode === "exchange" ? (
              <ExchangeOfferPanel
                offers={exchangeOffersQuery.data || []}
                loading={exchangeOffersQuery.isLoading || exchangeOffersQuery.isFetching}
                error={exchangeOffersQuery.error instanceof Error ? exchangeOffersQuery.error.message : undefined}
                onAdd={addExchangeOffer}
              />
            ) : null}

            <Collapse
              className="manual-entry-collapse"
              size="small"
              items={[
                {
                  key: "quick-check",
                  label: "活动看板逐项验收（真实商品与规则）",
                  children: (
                    <div className="activity-demo-picker">
                      <Select
                        className="full-width"
                        value={loadedDemoKey}
                        placeholder="选择一个活动，自动载入商品、日期和业务条件"
                        options={demoCaseOptions}
                        onChange={(key) => {
                          const demo = demoCases.find((item) => item.key === key);
                          if (demo) loadDemo(demo);
                        }}
                      />
                      {activeDemo ? (
                        <Alert
                          type="info"
                          showIcon
                          message={activeDemo.label}
                          description={`${activeDemo.description} 已载入，点击右上角“一键计算促销”验收。`}
                        />
                      ) : null}
                      <Button href="/operation-campaigns" icon={<GiftOutlined />}>
                        会员发券、积分与权益包验收
                      </Button>
                    </div>
                  )
                },
                {
                  key: "manual",
                  label: "找不到商品时手动录入",
                  forceRender: true,
                  children: (
                    <Form<ProductForm>
                      form={productForm}
                      layout="vertical"
                      initialValues={{
                        productCode: "amount-sku",
                        barcode: "barcode-amount",
                        name: "满减商品",
                        unitPrice: 120,
                        quantity: 1,
                        category: "家庭食品"
                      }}
                      onFinish={addItem}
                    >
                      <div className="compact-field-grid">
                        <Form.Item label="商品编码" name="productCode" rules={[{ required: true, message: "请输入商品编码" }]}>
                          <Input />
                        </Form.Item>
                        <Form.Item label="商品条码" name="barcode">
                          <Input />
                        </Form.Item>
                        <Form.Item label="商品名称" name="name" rules={[{ required: true, message: "请输入商品名称" }]}>
                          <Input />
                        </Form.Item>
                        <Form.Item label="单价" name="unitPrice" rules={[{ required: true, message: "请输入单价" }]}>
                          <InputNumber min={0} precision={2} className="full-width" />
                        </Form.Item>
                        <Form.Item label="数量" name="quantity" rules={[{ required: true, message: "请输入数量" }]}>
                          <InputNumber min={1} precision={0} className="full-width" />
                        </Form.Item>
                        <Form.Item label="品类" name="category">
                          <Input />
                        </Form.Item>
                      </div>
                      <Button type="primary" icon={<PlusOutlined />} htmlType="submit">
                        添加商品
                      </Button>
                    </Form>
                  )
                }
              ]}
            />
          </section>

          <section className="panel">
            <Space className="panel-toolbar" align="center">
              <Typography.Title level={3} className="section-title">
                购物车
              </Typography.Title>
              <Space>
                <Button disabled={cartItems.length === 0} onClick={startNewCheckout}>
                  清空
                </Button>
                <Tag color="blue">{cartItems.length} 行</Tag>
              </Space>
            </Space>
            {cartItems.length === 0 ? (
              <EmptyState description="购物车为空，请扫码或载入示例" />
            ) : (
              <>
                <Table<CartItem>
                  rowKey="lineId"
                  size="small"
                  columns={cartColumns}
                  dataSource={cartItems}
                  pagination={false}
                  scroll={{ x: 760 }}
                />
                <div className="cart-total-bar">
                  <span>商品合计</span>
                  <Price amount={cartTotal} size="medium" />
                </div>
              </>
            )}
          </section>

          <section className="panel">
            <Typography.Title level={3} className="section-title">
              业务信息
            </Typography.Title>
            <div className="cashier-context-row">
              <Form<CustomerForm>
                form={customerForm}
                layout="vertical"
                initialValues={{ member: true, memberLevel: "gold", memberCode: "demo-member-002" }}
              >
                <Space size={10} align="start" wrap>
                  <Form.Item name="member" valuePropName="checked" label="会员">
                    <Checkbox>是</Checkbox>
                  </Form.Item>
                  <Form.Item label="会员编号" name="memberCode">
                    <Input style={{ width: 150 }} />
                  </Form.Item>
                  <Form.Item label="会员等级" name="memberLevel">
                    <Select style={{ width: 128 }} options={memberLevelOptions} />
                  </Form.Item>
                </Space>
              </Form>
              <Form<FuelForm> form={fuelForm} layout="vertical" initialValues={fuel()}>
                <Space size={10} align="start" wrap>
                  <Form.Item label="油品类型" name="fuelType">
                    <Select style={{ width: 132 }} options={fuelTypeOptions} />
                  </Form.Item>
                  <Form.Item label="油品牌号" name="fuelGrade">
                    <Input style={{ width: 104 }} />
                  </Form.Item>
                  <Form.Item label="油品金额" name="amount">
                    <InputNumber min={0} precision={2} style={{ width: 126 }} />
                  </Form.Item>
                  <Form.Item label="油品升数" name="volume">
                    <InputNumber min={0} precision={2} style={{ width: 126 }} />
                  </Form.Item>
                </Space>
              </Form>
            </div>

            <Collapse
              size="small"
              items={[
                {
                  key: "advanced",
                  label: "验收日期、站点、充值与优惠券",
                  forceRender: true,
                  children: (
                    <Form<TransactionContextForm> form={contextForm} layout="vertical" initialValues={defaultContext()}>
                      <div className="compact-field-grid context-fields">
                        <Form.Item label="验收日期" name="businessDate">
                          <Input type="date" />
                        </Form.Item>
                        <Form.Item label="验收时间" name="businessTime">
                          <Input type="time" step={1} />
                        </Form.Item>
                        <Form.Item label="当前站点" name="stationCode">
                          <Select
                            showSearch
                            loading={stationsQuery.isLoading}
                            options={stationOptions}
                            optionFilterProp="label"
                          />
                        </Form.Item>
                        <Form.Item label="站点类型" name="stationType">
                          <Select options={stationTypeOptions} />
                        </Form.Item>
                        <Form.Item label="省区" name="stationProvince">
                          <Input />
                        </Form.Item>
                        <Form.Item label="会员生日月" name="memberBirthMonth">
                          <Select
                            allowClear
                            options={Array.from({ length: 12 }, (_, index) => ({
                              value: index + 1,
                              label: `${index + 1}月`
                            }))}
                          />
                        </Form.Item>
                        <Form.Item label="充值金额" name="rechargeAmount">
                          <InputNumber min={0} precision={2} className="full-width" />
                        </Form.Item>
                        <Form.Item label="支付方式" name="paymentMethod">
                          <Select
                            options={[
                              { value: "E_ENJOY_CARD", label: "昆仑 e 享卡" },
                              { value: "CASH", label: "现金" },
                              { value: "OTHER", label: "其他" }
                            ]}
                          />
                        </Form.Item>
                        <Form.Item label="券 ID" name="couponId">
                          <Input />
                        </Form.Item>
                        <Form.Item label="已选券 ID" name="selectedCouponIds">
                          <Input placeholder="多个券 ID 用逗号分隔" />
                        </Form.Item>
                        <Form.Item label="券名称" name="couponName">
                          <Input />
                        </Form.Item>
                        <Form.Item label="券面额" name="couponFaceValue">
                          <InputNumber min={0} precision={2} className="full-width" />
                        </Form.Item>
                        <Form.Item label="满额门槛" name="couponMinSpendAmount">
                          <InputNumber min={0} precision={2} className="full-width" />
                        </Form.Item>
                        <Form.Item label="适用品类" name="couponApplicableCategories">
                          <Input />
                        </Form.Item>
                        <Form.Item label="排除品类" name="couponExcludedCategories">
                          <Input />
                        </Form.Item>
                        <Form.Item name="couponStackable" valuePropName="checked" label="券叠加">
                          <Checkbox>允许</Checkbox>
                        </Form.Item>
                      </div>
                    </Form>
                  )
                }
              ]}
            />
          </section>
        </div>

        <div className="checkout-column result-column">
          {checkoutMutation.error ? (
            <Alert
              type="error"
              showIcon
              message="促销计算失败"
              description={checkoutMutation.error.message}
              className="result-alert"
            />
          ) : null}
          {confirmMutation.error ? (
            <Alert
              type="error"
              showIcon
              message="确认结算失败"
              description={confirmMutation.error.message}
              className="result-alert"
            />
          ) : null}

          {!result ? (
            <section className="panel waiting-panel">
              <div className="waiting-icon">
                <ShoppingCartOutlined />
              </div>
              <Typography.Title level={3} className="section-title">
                等待计算
              </Typography.Title>
              <div className="mini-ledger">
                <div>
                  <span>商品件数</span>
                  <strong>{cartCount}</strong>
                </div>
                <div>
                  <span>当前金额</span>
                  <Price amount={transactionInputTotal} size="medium" />
                </div>
                <div>
                  <span>下一步</span>
                  <strong>{hasTransactionInput ? "一键计算促销" : "扫码加入商品"}</strong>
                </div>
              </div>
            </section>
          ) : null}

          {result ? (
            <>
              {activeDemo ? <ScenarioAcceptance demo={activeDemo} result={result} /> : null}
              <PromotionOutcome result={result} recommended={recommended} />

              <section className="panel">
                <Typography.Title level={3} className="section-title">
                  可用促销方案
                </Typography.Title>
                {promotionCandidates.length === 0 ? (
                  <EmptyState description="没有可用促销" />
                ) : (
                  <div className="candidate-list">
                    {promotionCandidates.map((candidate) => (
                      <CandidateCard
                        key={candidate.candidateId}
                        candidate={candidate}
                        selected={selectedCandidateId === candidate.candidateId}
                        recommended={candidate.candidateId === recommended?.candidateId}
                        onSelect={() => setSelectedCandidateId(candidate.candidateId)}
                      />
                    ))}
                  </div>
                )}
              </section>

              <section className="panel original-choice">
                <Space className="panel-toolbar" align="start">
                  <div>
                    <Typography.Title level={3} className="section-title">
                      原价兜底
                    </Typography.Title>
                    <Typography.Text type="secondary">
                      {result.originalPriceFallback.explanation || "顾客不使用促销时选择此方案"}
                    </Typography.Text>
                  </div>
                  <Button className="blue-button" onClick={() => setSelectedCandidateId(result.originalPriceFallback.candidateId)}>
                    选择原价方案
                  </Button>
                </Space>
                <Price amount={result.originalPriceFallback.payableAmount} size="medium" />
              </section>

              <section className="panel confirm-panel">
                <Space className="panel-toolbar" align="start">
                  <div>
                    <Typography.Title level={3} className="section-title">
                      确认结算
                    </Typography.Title>
                    <Typography.Text type="secondary">当前选择：{selectedCandidate?.title || result.originalPriceFallback.title}</Typography.Text>
                    {latestConfirmationId ? (
                      <div className="confirmation-done">
                        <CheckCircleOutlined />
                        <span>已锁定：{latestConfirmationId}</span>
                      </div>
                    ) : null}
                  </div>
                  <Space>
                    <Button
                      size="large"
                      type="primary"
                      icon={<CheckCircleOutlined />}
                      loading={confirmMutation.isPending}
                      disabled={!selectedCandidateId || Boolean(latestConfirmationId)}
                      onClick={confirmSettlement}
                    >
                      确认收款
                    </Button>
                    <Button icon={<PrinterOutlined />} disabled={!latestConfirmationId} onClick={() => api.info("打印功能待接入")}>
                      打印
                    </Button>
                  </Space>
                </Space>
              </section>

              <PromotionProblems result={result} />

              <section className="panel audit-compact">
                <Collapse
                  size="small"
                  items={[
                    {
                      key: "audit",
                      label: "审计追踪",
                      children: (
                        <Space direction="vertical" size={6} className="full-width">
                          <Typography.Text>calculationId: {result.calculationId}</Typography.Text>
                          <Typography.Text>ruleVersionIds: {result.ruleVersionIds.join(", ") || "-"}</Typography.Text>
                        </Space>
                      )
                    }
                  ]}
                />
              </section>
            </>
          ) : null}
        </div>
      </div>
    </>
  );
}

function PromotionOutcome({ result, recommended }: { result: CheckoutCalculateResponse; recommended?: Candidate }) {
  const saved = toNumber(result.discountAmount);
  const original = toNumber(result.originalAmount);
  const savedRate = original > 0 ? Math.round((saved / original) * 100) : 0;
  const hasPromotion = Boolean(recommended && recommended.ruleType !== "ORIGINAL_PRICE");
  const warnings = result.warnings.length + result.inventoryWarnings.length;

  return (
    <section className={`panel promotion-outcome ${hasPromotion ? "hit" : "fallback"}`}>
      <Space className="panel-toolbar" align="start">
        <div>
          <Typography.Title level={3} className="section-title">
            推荐方案
          </Typography.Title>
          <Typography.Text strong>{recommended?.title || result.originalPriceFallback.title}</Typography.Text>
          <br />
          <Typography.Text type="secondary">{result.explanations.join(" ") || "后端已返回本单结算结果。"}</Typography.Text>
        </div>
        <Tag color={hasPromotion ? "green" : "default"}>{hasPromotion ? "已命中活动" : "原价结算"}</Tag>
      </Space>

      <div className="payable-card">
        <div>
          <Typography.Text>顾客到手价</Typography.Text>
          <Price amount={result.payableAmount} size="large" variant="promo" />
        </div>
        <div className="payable-saving">
          <span>本单优惠</span>
          <strong>{saved > 0 ? `省 ${formatMoney(saved)}` : "赠品/赠券权益"}</strong>
          <small>{savedRate > 0 ? `约 ${savedRate}%` : hasPromotion ? "本单已命中活动" : "无优惠"}</small>
        </div>
      </div>

      <div className="feedback-grid">
        <FeedbackTile label="原价" value={<Price amount={result.originalAmount} size="medium" variant="muted" strike={saved > 0} />} />
        <FeedbackTile label="可选活动" value={`${uniquePromotionCandidates(result.availableCandidates).length} 个`} />
        <FeedbackTile label="不可用活动" value={`${result.blockedPromotions.length} 个`} tone={result.blockedPromotions.length ? "warning" : "normal"} />
        <FeedbackTile label="库存/系统提醒" value={`${warnings} 条`} tone={warnings ? "warning" : "normal"} />
      </div>

      <div className="promotion-talk">
        <GiftOutlined />
        <span>
          {hasPromotion
            ? saved > 0
              ? `已为顾客选择“${recommended?.title}”，少付 ${formatMoney(saved)}。`
              : `已为顾客命中“${recommended?.title}”，请核对赠品、赠券和积分权益。`
            : "本单没有产生优惠，可按原价继续收款。"}
        </span>
      </div>

      {recommended ? (
        <div className="reward-strip">
          {renderGifts(recommended.gifts)}
          {renderCoupons(recommended.coupons)}
          {recommended.consumedCouponIds.length > 0 ? <Tag color="purple">核销券 {recommended.consumedCouponIds.join(", ")}</Tag> : null}
          {recommended.pointsMultiplier > 1 ? <Tag color="cyan">积分 x {recommended.pointsMultiplier}</Tag> : null}
        </div>
      ) : null}
      {result.pointsPreview ? (
        <Alert
          type="success"
          showIcon
          message={`${result.pointsPreview.activityName}：${result.pointsPreview.multiplier} 倍积分`}
          description={`确认收款后预计增加 ${result.pointsPreview.estimatedPoints} 积分。`}
        />
      ) : null}
    </section>
  );
}

function ScenarioAcceptance({ demo, result }: { demo: DemoCase; result: CheckoutCalculateResponse }) {
  const hitRuleIds = new Set(result.availableCandidates.map((candidate) => candidate.ruleId));
  const missingRules = demo.expectedRuleIds.filter((ruleId) => !hitRuleIds.has(ruleId));
  const targetCandidates = result.availableCandidates.filter((candidate) => demo.expectedRuleIds.includes(candidate.ruleId));
  const missingGiftOptions = (demo.expectedGiftOptions || []).filter((expectedOption) =>
    !targetCandidates.some((candidate) => {
      const giftCodes = new Set(candidate.gifts.map((gift) => gift.productCode));
      return expectedOption.every((productCode) => giftCodes.has(productCode));
    })
  );
  const targetCoupons = targetCandidates.flatMap((candidate) => candidate.coupons);
  const missingCoupons = (demo.expectedCoupons || []).filter((expected) =>
    !targetCoupons.some((coupon) =>
      coupon.couponName === expected.name
      && Number(coupon.amount) === expected.amount
      && Number(coupon.quantity) === expected.quantity
    )
  );
  const pointsMismatch = demo.expectedPointsMultiplier !== undefined
    && Number(result.pointsPreview?.multiplier) !== demo.expectedPointsMultiplier;
  const accepted = missingRules.length === 0
    && missingGiftOptions.length === 0
    && missingCoupons.length === 0
    && !pointsMismatch;
  const benefitChecks = [
    demo.expectedGiftOptions?.length ? `${demo.expectedGiftOptions.length} 组赠品` : "",
    demo.expectedCoupons?.length ? `${demo.expectedCoupons.length} 类赠券` : "",
    demo.expectedPointsMultiplier ? `${demo.expectedPointsMultiplier} 倍积分` : ""
  ].filter(Boolean).join("、");
  const successDetail = `已命中 ${demo.expectedRuleIds.length} 条目标规则${benefitChecks ? `，并核对 ${benefitChecks}` : ""}。`;
  const warningDetail = [
    missingRules.length ? `未命中目标规则：${missingRules.join("、")}` : "",
    missingGiftOptions.length ? `缺少 ${missingGiftOptions.length} 组赠品方案。` : "",
    missingCoupons.length ? `缺少 ${missingCoupons.length} 类目标赠券。` : "",
    pointsMismatch ? `未返回 ${demo.expectedPointsMultiplier} 倍积分。` : ""
  ].filter(Boolean).join(" ");
  return (
    <Alert
      className="scenario-acceptance"
      type={accepted ? "success" : "warning"}
      showIcon
      message={accepted ? `验收通过：${demo.label}` : `请复核：${demo.label}`}
      description={accepted ? successDetail : warningDetail}
    />
  );
}

function FeedbackTile({
  label,
  value,
  tone = "normal"
}: {
  label: string;
  value: string | ReactNode;
  tone?: "normal" | "warning";
}) {
  return (
    <div className={`feedback-tile ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function CandidateCard({
  candidate,
  selected,
  recommended,
  onSelect
}: {
  candidate: Candidate;
  selected: boolean;
  recommended: boolean;
  onSelect: () => void;
}) {
  return (
    <div
      className={`candidate-card${selected ? " selected" : ""}${recommended ? " recommended" : ""}`}
      role="button"
      tabIndex={0}
      aria-pressed={selected}
      onClick={onSelect}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          onSelect();
        }
      }}
    >
      <div className="candidate-card-header">
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{candidate.title}</Typography.Text>
          <Typography.Text type="secondary">
            {ruleTypeLabel(candidate.ruleType)} / {candidate.ruleVersionId}
          </Typography.Text>
        </Space>
        <Space>
          {recommended ? <Tag color="gold">推荐</Tag> : null}
          <Tag color={candidate.stackable ? "blue" : "orange"}>{candidate.stackable ? "可叠加" : "互斥"}</Tag>
          {selected ? <Tag color="green">已选择</Tag> : null}
        </Space>
      </div>
      <div className="candidate-money">
        <div>
          <Typography.Text type="secondary">原价</Typography.Text>
          <br />
          <Price amount={candidate.originalAmount} size="small" variant="muted" strike />
        </div>
        <div>
          <Typography.Text type="secondary">到手价</Typography.Text>
          <br />
          <Price amount={candidate.payableAmount} size="medium" variant="promo" />
        </div>
        <div>
          <Typography.Text type="secondary">优惠</Typography.Text>
          <br />
          <Price amount={candidate.discountAmount} size="small" variant="success" />
        </div>
      </div>
      <Divider style={{ margin: "8px 0" }} />
      <Space direction="vertical" size={4}>
        <Typography.Text>{candidate.explanation}</Typography.Text>
        <Space wrap>
          {renderGifts(candidate.gifts)}
          {renderCoupons(candidate.coupons)}
          {candidate.pointsMultiplier > 1 ? <Tag color="cyan">积分 x {candidate.pointsMultiplier}</Tag> : null}
        </Space>
        {candidate.consumedCouponIds.length > 0 ? (
          <Typography.Text type="secondary">核销券：{candidate.consumedCouponIds.join(", ")}</Typography.Text>
        ) : null}
      </Space>
    </div>
  );
}

function PromotionProblems({ result }: { result: CheckoutCalculateResponse }) {
  const hasWarnings = result.warnings.length > 0 || result.inventoryWarnings.length > 0;
  const [query, setQuery] = useState("");
  const [visibleCount, setVisibleCount] = useState(12);
  const filteredPromotions = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase();
    if (!keyword) {
      return result.blockedPromotions;
    }
    return result.blockedPromotions.filter((promotion) =>
      promotion.title.toLocaleLowerCase().includes(keyword)
      || promotion.ruleId.toLocaleLowerCase().includes(keyword)
      || promotion.blockedReasons.some((reason) => reason.message.toLocaleLowerCase().includes(keyword))
    );
  }, [query, result.blockedPromotions]);
  const visiblePromotions = filteredPromotions.slice(0, visibleCount);

  useEffect(() => {
    setQuery("");
    setVisibleCount(12);
  }, [result.calculationId]);

  return (
    <section className="panel">
      <Space className="panel-toolbar" align="center">
        <div>
          <Typography.Title level={3} className="section-title">未命中活动</Typography.Title>
          <Typography.Text type="secondary">搜索活动名称，查看具体未满足条件</Typography.Text>
        </div>
        <Tag>{result.blockedPromotions.length} 条</Tag>
      </Space>
      {result.blockedPromotions.length === 0 ? (
        <EmptyState description="没有未命中活动" />
      ) : (
        <>
          <Input
            allowClear
            prefix={<SearchOutlined />}
            value={query}
            placeholder="搜索活动名称、规则编号或未满足条件"
            onChange={(event) => {
              setQuery(event.target.value);
              setVisibleCount(12);
            }}
            className="blocked-promotion-search"
          />
          {visiblePromotions.length === 0 ? (
            <EmptyState description="没有匹配的活动" />
          ) : (
            <Collapse
              items={visiblePromotions.map((promotion) => ({
                key: `${promotion.ruleId}-${promotion.ruleVersionId || "unversioned"}`,
                label: `${promotion.title} / ${ruleTypeLabel(promotion.ruleType)}`,
                children: <BlockedReasons promotion={promotion} />
              }))}
            />
          )}
          {visiblePromotions.length < filteredPromotions.length ? (
            <Button className="show-more-promotions" onClick={() => setVisibleCount((count) => count + 20)}>
              显示更多（剩余 {filteredPromotions.length - visiblePromotions.length} 条）
            </Button>
          ) : null}
        </>
      )}
      {hasWarnings ? (
        <>
          <Divider />
          <Space direction="vertical" size={8} className="full-width">
            {result.warnings.map((warning) => (
              <Alert key={warning} type="warning" showIcon message={warning} />
            ))}
            {result.inventoryWarnings.map((warning) => (
              <InventoryWarningLine key={`${warning.productCode}-${warning.message}`} warning={warning} />
            ))}
          </Space>
        </>
      ) : null}
    </section>
  );
}

function BlockedReasons({ promotion }: { promotion: BlockedPromotion }) {
  return (
    <Space direction="vertical" size={6} className="full-width">
      {promotion.blockedReasons.map((reason, index) => (
        <div className="blocked-reason" key={`${reason.code}-${index}`}>
          <WarningOutlined />
          <span>{reason.message || reason.code}</span>
          <Tag>{reason.code}</Tag>
        </div>
      ))}
    </Space>
  );
}

function InventoryWarningLine({ warning }: { warning: InventoryWarning }) {
  return <Alert type="warning" showIcon message={`${warning.productCode}：${warning.message}`} />;
}

function StepPill({ index, title, active, done }: { index: number; title: string; active: boolean; done: boolean }) {
  return (
    <div className={`step-pill${active ? " active" : ""}${done ? " done" : ""}`}>
      <span>{done ? <CheckCircleOutlined /> : index}</span>
      <strong>{title}</strong>
    </div>
  );
}

function ModeContext({ mode }: { mode: CheckoutMode }) {
  const content: Record<CheckoutMode, { title: string; detail: string }> = {
    shop: { title: "便利店商品", detail: "扫码或搜索商品，计算满减、固定价、赠品与赠券" },
    fuel: { title: "加油站", detail: "在业务信息中录入油品类型、金额和升数" },
    exchange: { title: "加油换购", detail: "录入油品金额后，从可换购商品中选择符合条件的商品" },
    coupon: { title: "用券结算", detail: "录入会员和优惠券信息，系统自动核对可用范围与叠加条件" }
  };
  return (
    <div className="mode-context">
      <strong>{content[mode].title}</strong>
      <span>{content[mode].detail}</span>
    </div>
  );
}

function ExchangeOfferPanel({
  offers,
  loading,
  error,
  onAdd
}: {
  offers: CheckoutExchangeOffer[];
  loading: boolean;
  error?: string;
  onAdd: (offer: CheckoutExchangeOffer) => void;
}) {
  const eligibleCount = offers.filter((offer) => offer.eligible).length;
  return (
    <div className="exchange-offer-panel">
      <Space className="panel-toolbar" align="center">
        <div>
          <Typography.Title level={4} className="compact-title">
            可换购商品
          </Typography.Title>
          <Typography.Text type="secondary">来自已确认 EXCHANGE_PURCHASE 规则和商品目录</Typography.Text>
        </div>
        <Space>
          <Tag color="green">{eligibleCount} 个可用</Tag>
          <Tag>{offers.length} 条规则</Tag>
        </Space>
      </Space>

      {error ? <Alert type="error" showIcon message="换购清单加载失败" description={error} className="result-alert" /> : null}
      {loading ? <Alert type="info" showIcon message="正在查询当前可换购商品" className="result-alert" /> : null}
      {!loading && offers.length === 0 ? <EmptyState description="当前没有已发布的换购商品" /> : null}

      <div className="exchange-offer-list">
        {offers.map((offer) => (
          <div className={`exchange-offer-card${offer.eligible ? " eligible" : " blocked"}`} key={`${offer.ruleId}-${offer.productCode}`}>
            <div className="exchange-offer-main">
              <Space direction="vertical" size={2}>
                <Typography.Text strong>{offer.productName}</Typography.Text>
                <Typography.Text type="secondary">
                  {offer.productCode} / {offer.activityName}
                </Typography.Text>
              </Space>
              <Tag color={offer.eligible ? "green" : "orange"}>{offer.eligible ? "可加入" : "未满足"}</Tag>
            </div>
            <div className="exchange-offer-facts">
              <span>
                油品满 <Price amount={offer.minFuelAmount} size="small" />
              </span>
              <span>
                原价 <Price amount={offer.unitPrice} size="small" /> / 换购 <Price amount={offer.exchangePrice} size="small" />
              </span>
              <span>数量 {offer.exchangeQuantity}</span>
              <span>库存 {Number(offer.inventoryQuantity || 0)}</span>
              <span>
                预计优惠 <Price amount={offer.estimatedDiscount} size="small" />
              </span>
            </div>
            {offer.blockedReasons.length ? (
              <Typography.Text type="secondary" className="exchange-blocked-reason">
                {offer.blockedReasons[0]}
              </Typography.Text>
            ) : null}
            <Button type={offer.eligible ? "primary" : "default"} disabled={!offer.eligible} onClick={() => onAdd(offer)}>
              加入换购
            </Button>
          </div>
        ))}
      </div>
    </div>
  );
}

function item(
  productCode: string,
  name: string,
  quantity: number,
  unitPrice: number,
  category: string,
  inventoryQuantity = 20
): CartItem {
  return {
    lineId: `line-${productCode}`,
    productCode,
    barcode: `barcode-${productCode}`,
    name,
    quantity,
    unitPrice,
    category,
    inventoryQuantity
  };
}

function fuel(fuelType: FuelType = "NONE", fuelGrade = "", amount = 0, volume = 0): FuelForm {
  return { fuelType, fuelGrade, amount, volume };
}

function defaultContext(): TransactionContextForm {
  const current = currentBusinessTime();
  return {
    stationCode: "1-A6501-C001-S001",
    stationType: "gas_station",
    stationProvince: "新疆",
    businessDate: current.businessDate,
    businessTime: current.businessTime,
    rechargeAmount: 0,
    paymentMethod: "E_ENJOY_CARD",
    memberBirthMonth: 7,
    couponStackable: false
  };
}

function splitCsv(value?: string) {
  return (value || "")
    .split(",")
    .map((part) => part.trim())
    .filter(Boolean);
}

function looksLikeBarcode(value: string) {
  return /^[0-9]{8,18}$/.test(value);
}

function resolveSelectedCouponIds(values: TransactionContextForm) {
  const selected = splitCsv(values.selectedCouponIds);
  if (selected.length > 0) {
    return selected;
  }
  return values.couponId ? [values.couponId] : [];
}

function buildAvailableCoupons(values: TransactionContextForm): Coupon[] {
  if (!values.couponId) {
    return [];
  }
  return [
    {
      couponId: values.couponId,
      couponTemplateId: `template-${values.couponId}`,
      couponName: values.couponName || values.couponId,
      faceValue: Number(values.couponFaceValue || 0),
      minSpendAmount: Number(values.couponMinSpendAmount || 0),
      applicableCategories: splitCsv(values.couponApplicableCategories),
      excludedCategories: splitCsv(values.couponExcludedCategories),
      applicableProductCodes: [],
      excludedProductCodes: [],
      validFrom: values.businessDate || currentBusinessTime().businessDate,
      validUntil: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
      memberOnly: false,
      stackable: Boolean(values.couponStackable),
      status: "AVAILABLE",
      operatorId: "cashier-input",
      holderMemberId: ""
    }
  ];
}

function renderGifts(gifts: GiftItem[]) {
  if (!gifts.length) {
    return null;
  }
  return gifts.map((gift) => (
    <Tag key={gift.productCode} color="green">
      赠品 {gift.name} x {gift.quantity}
    </Tag>
  ));
}

function renderCoupons(coupons: GiftCoupon[]) {
  if (!coupons.length) {
    return null;
  }
  return coupons.map((coupon) => (
    <Tag key={coupon.couponName} color="blue">
      赠券 {coupon.couponName} <Price amount={coupon.amount} size="small" />
    </Tag>
  ));
}

function currentBusinessTime() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  return {
    businessDate: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`,
    businessTime: `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  };
}

function normalizeBusinessTime(value: string) {
  return /^\d{2}:\d{2}$/.test(value) ? `${value}:00` : value;
}

function uniquePromotionCandidates(candidates: Candidate[]) {
  const unique = new Map<string, Candidate>();
  candidates
    .filter((candidate) => candidate.ruleType !== "ORIGINAL_PRICE")
    .forEach((candidate) => {
      const signature = JSON.stringify({
        ruleType: candidate.ruleType,
        payableAmount: toNumber(candidate.payableAmount),
        discountAmount: toNumber(candidate.discountAmount),
        gifts: candidate.gifts,
        coupons: candidate.coupons,
        compositeComponents: candidate.compositeComponents || [],
        pointsMultiplier: candidate.pointsMultiplier
      });
      if (!unique.has(signature)) {
        unique.set(signature, candidate);
      }
    });
  return Array.from(unique.values());
}

function lineTotal(record: CartItem) {
  return Number(record.unitPrice || 0) * Number(record.quantity || 0);
}

function sortProductResults(products: ProductCatalogItem[], query: string): ProductCatalogItem[] {
  const keyword = query.trim().toLocaleLowerCase();
  return [...products].sort((left, right) => {
    const leftStocked = Number(left.inventoryQuantity || 0) > 0 ? 0 : 1;
    const rightStocked = Number(right.inventoryQuantity || 0) > 0 ? 0 : 1;
    if (leftStocked !== rightStocked) {
      return leftStocked - rightStocked;
    }
    const leftName = left.productName.toLocaleLowerCase();
    const rightName = right.productName.toLocaleLowerCase();
    const leftPrefix = keyword && leftName.startsWith(keyword) ? 0 : 1;
    const rightPrefix = keyword && rightName.startsWith(keyword) ? 0 : 1;
    if (leftPrefix !== rightPrefix) {
      return leftPrefix - rightPrefix;
    }
    return left.productCode.localeCompare(right.productCode);
  });
}

function sumCart(items: CartItem[]) {
  return items.reduce((total, cartItem) => total + lineTotal(cartItem), 0);
}

function toNumber(value: number | string | null | undefined) {
  return Number(value || 0);
}

function formatMoney(value: number) {
  return `¥${value.toFixed(2)}`;
}

function ruleTypeLabel(value: string) {
  const labels: Record<string, string> = {
    FIXED_PRICE: "固定价",
    AMOUNT_OFF: "满减",
    GIFT_ITEM: "买赠",
    GIFT_COUPON: "赠券",
    BUNDLE_PRICE: "组合包",
    EXCHANGE_PURCHASE: "加油换购",
    COUPON_REDEEM: "券核销",
    FUEL_VOLUME_DISCOUNT: "油品升数优惠",
    PERCENTAGE_DISCOUNT: "折扣"
  };
  return labels[value] || value;
}
