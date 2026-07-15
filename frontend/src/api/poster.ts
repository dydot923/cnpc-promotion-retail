export type PosterActivity = {
  activityId: string;
  activityName: string;
  description: string;
};

export type PosterProduct = {
  productCode: string;
  productName: string;
  displayPrice: string;
};

export type PosterTask = {
  taskId: string;
  imageUrl?: string;
  prompt: string;
  status: "PENDING_CONFIRMATION" | "APPROVED";
  generatedAt: string;
};

export const posterActivities: PosterActivity[] = [
  {
    activityId: "activity-99-zone",
    activityName: "9.9 专区",
    description: "便利店固定价促销，适合收银台展架与门店海报"
  },
  {
    activityId: "activity-exchange",
    activityName: "加油换购",
    description: "油非联动活动，适合加油岛和便利店入口"
  },
  {
    activityId: "activity-member-day",
    activityName: "会员日满减",
    description: "会员专享活动，适合站内电子屏"
  }
];

export const posterProducts: Record<string, PosterProduct[]> = {
  "activity-99-zone": [
    { productCode: "fixed-sku", productName: "矿泉水", displayPrice: "9.90" },
    { productCode: "snack-01", productName: "休闲零食", displayPrice: "9.90" }
  ],
  "activity-exchange": [
    { productCode: "70545526", productName: "换购水饮", displayPrice: "4.00" },
    { productCode: "oil-care-01", productName: "车辅用品", displayPrice: "19.90" }
  ],
  "activity-member-day": [
    { productCode: "amount-sku", productName: "家庭食品", displayPrice: "99.00" },
    { productCode: "coupon-sku", productName: "赠券商品", displayPrice: "30.00" }
  ]
};

export function buildPosterPrompt(activity: PosterActivity, products: PosterProduct[]) {
  const productText = products.map((product) => `${product.productName} 到手价 ¥${product.displayPrice}`).join("，");
  return `促销活动：${activity.activityName}；商品：${productText}；风格：中石油红蓝黄品牌色，柜台人员可直接用于门店海报审核。`;
}

export function createPosterTask(activity: PosterActivity, products: PosterProduct[]): PosterTask {
  return {
    taskId: `poster-${Date.now()}`,
    prompt: buildPosterPrompt(activity, products),
    status: "PENDING_CONFIRMATION",
    generatedAt: new Date().toISOString()
  };
}
