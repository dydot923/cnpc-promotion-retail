export type FuelType = "NONE" | "GASOLINE" | "DIESEL" | "CNG" | "LNG" | "CN98";

export type CartItem = {
  lineId: string;
  productCode: string;
  barcode?: string | null;
  name: string;
  quantity: number;
  unitPrice: number;
  category?: string | null;
  inventoryQuantity?: number | null;
};

export type Coupon = {
  couponId: string;
  couponTemplateId: string;
  couponName: string;
  faceValue: number;
  minSpendAmount: number;
  applicableCategories: string[];
  excludedCategories: string[];
  applicableProductCodes: string[];
  excludedProductCodes: string[];
  validFrom?: string | null;
  validUntil?: string | null;
  memberOnly: boolean;
  stackable: boolean;
  status: "AVAILABLE" | "USED" | "EXPIRED" | "DISABLED";
  issuedAt?: string | null;
  usedAt?: string | null;
  operatorId?: string | null;
  holderMemberId?: string | null;
};

export type OrderContext = {
  station: {
    stationId: string;
    stationType: string;
    region?: string | null;
  };
  customer: {
    member: boolean;
    memberLevel?: string | null;
    availableCouponIds: string[];
    memberBirthMonth?: number | null;
    memberCode?: string | null;
  };
  fuel: {
    fuelType: FuelType;
    fuelGrade?: string | null;
    amount: number;
    volume: number;
  };
  cartItems: CartItem[];
  businessDate: string;
  businessTime: string;
  availableCoupons?: Coupon[];
  rechargeAmount?: number;
};

export type CheckoutCalculateRequest = {
  orderContext: OrderContext;
  transactionDate?: string;
  transactionTime?: string;
  stationType?: string;
  stationProvince?: string;
  stationCity?: string;
  stationCode?: string;
  isMember?: boolean;
  memberLevel?: string | null;
  memberCode?: string | null;
  memberBirthMonth?: number | null;
  paymentMethod?: string;
  fuelType?: FuelType;
  fuelAmount?: number;
  fuelVolume?: number;
  rechargeAmount?: number;
  availableCoupons?: Coupon[];
  selectedCouponIds?: string[];
};

export type GiftItem = {
  productCode: string;
  name: string;
  quantity: number;
};

export type GiftCoupon = {
  couponName: string;
  amount: number;
  quantity: number;
  useThreshold: number;
  validDays: number;
};

export type CompositeBenefitComponent = {
  type: string;
  title?: string | null;
  amount?: number | null;
  quantity?: number | null;
};

export type Candidate = {
  candidateId: string;
  ruleId: string;
  title: string;
  ruleType: string;
  status: string;
  originalAmount: number;
  payableAmount: number;
  discountAmount: number;
  gifts: GiftItem[];
  coupons: GiftCoupon[];
  explanation: string;
  ruleVersionId: string;
  stackable: boolean;
  exclusiveGroup?: string | null;
  consumedCouponIds: string[];
  compositeComponents?: CompositeBenefitComponent[];
  pointsMultiplier: number;
};

export type CandidateSnapshot = {
  candidateId: string;
  ruleId: string;
  title: string;
  ruleType: string;
  originalAmount: number;
  payableAmount: number;
  discountAmount: number;
  gifts: GiftItem[];
  coupons: GiftCoupon[];
  explanation: string;
  ruleVersion: string;
  exclusiveGroup?: string | null;
  stackable: boolean;
  priority: number;
  consumedProductCodes: string[];
  consumedCouponIds: string[];
  pointsMultiplier: number;
};

export type BlockedReason = {
  code: string;
  message: string;
};

export type BlockedPromotion = {
  ruleId: string;
  ruleType: string;
  title: string;
  blockedReasons: BlockedReason[];
  ruleVersionId?: string | null;
};

export type InventoryWarning = {
  productCode: string;
  message: string;
};

export type CheckoutCalculateResponse = {
  calculationId: string;
  originalAmount: number;
  payableAmount: number;
  discountAmount: number;
  recommendedCandidateId: string;
  availableCandidates: Candidate[];
  blockedPromotions: BlockedPromotion[];
  warnings: string[];
  explanations: string[];
  ruleVersion?: string | null;
  ruleVersionIds: string[];
  inventoryWarnings: InventoryWarning[];
  pointsPreview?: PointsPreview | null;
  originalPriceFallback: Candidate;
};

export type PointsPreview = {
  activityId: string;
  ruleId: string;
  activityName: string;
  multiplier: number;
  estimatedPoints: number;
};

export type Station = {
  stationCode: string;
  stationName: string;
  province: string;
  city?: string | null;
  district?: string | null;
  stationType: string;
};

export type CheckoutConfirmRequest = {
  orderNo?: string;
  calculationId: string;
  selectedCandidateId: string;
  skippedPromotion: boolean;
  operatorId?: string;
  operatorName?: string;
};

export type CheckoutConfirmationResponse = {
  confirmationId: string;
  calculationId: string;
  selectedCandidateId: string;
  selectedCandidateSnapshot: CandidateSnapshot;
  cartItems: CartItem[];
  operatorId: string;
  operatorName: string;
  skipped: boolean;
  confirmedAt: string;
};

export type CheckoutTransactionItem = {
  productCode: string;
  productName: string;
  barcode?: string | null;
  category?: string | null;
  unitPrice: number;
  actualPrice: number;
  quantity: number;
  subtotal: number;
  appliedPromoId?: string | null;
  appliedCouponCode?: string | null;
};

export type CheckoutTransactionResponse = {
  txnNo: string;
  confirmationId: string;
  calculationId: string;
  selectedCandidateId: string;
  totalAmount: number;
  discountAmount: number;
  payableAmount: number;
  paymentMethod?: string | null;
  operatorId: string;
  operatorName: string;
  memberCode?: string | null;
  stationCode?: string | null;
  status: string;
  createdAt: string;
  items: CheckoutTransactionItem[];
};

export type CheckoutExchangeOffer = {
  ruleId: string;
  activityName: string;
  ruleVersion: string;
  offerType?: "ITEM" | "BUNDLE";
  productCode: string;
  productName: string;
  barcode?: string | null;
  category?: string | null;
  unitPrice: number;
  exchangePrice: number;
  exchangeQuantity: number;
  minFuelAmount: number;
  fuelTypes: FuelType[];
  estimatedDiscount: number;
  inventoryQuantity: number;
  eligible: boolean;
  blockedReasons: string[];
  bundleItems?: {
    productCode: string;
    productName: string;
    barcode?: string | null;
    category?: string | null;
    unitPrice: number;
    quantity: number;
    inventoryQuantity: number;
  }[];
};

export type OperationCampaignDefinition = {
  campaignCode: string;
  campaignName: string;
  endpoint: string;
  benefitSummary: string;
  requiredFields: string[];
  optionalFields: string[];
};

export type OperationCouponIssueResponse = {
  activityCode: string;
  memberCode: string;
  eventKey: string;
  coupons: Coupon[];
};

export type MemberResponse = {
  memberCode: string;
  memberName: string;
  phone?: string | null;
  level: string;
  levelName: string;
  totalPoints: number;
  availablePoints: number;
  birthday?: string | null;
  province?: string | null;
  eEnjoyCardNo?: string | null;
  usualProvince?: string | null;
  registeredAt?: string | null;
  cardOpenedAt?: string | null;
  status: string;
  memberTags: string[];
  discountRate: number;
  pointsMultiplier: number;
  benefits: string[];
};

export type MemberIdentifyRequest = {
  identifier: string;
  identifyType?: "PHONE" | "MEMBER_CODE" | string;
};

export type MemberCreateRequest = {
  memberCode?: string;
  memberName: string;
  phone?: string;
  levelCode?: string;
  totalPoints?: number;
  availablePoints?: number;
  birthday?: string;
  province?: string;
  eEnjoyCardNo?: string;
  usualProvince?: string;
  registeredAt?: string;
  cardOpenedAt?: string;
  status?: string;
  memberTags?: string[];
  openedCard?: boolean;
};

export type MemberUpdateRequest = {
  memberName?: string;
  phone?: string;
  levelCode?: string;
  birthday?: string;
  province?: string;
  eEnjoyCardNo?: string;
  usualProvince?: string;
  registeredAt?: string;
  cardOpenedAt?: string;
  status?: string;
  memberTags?: string[];
  openedCard?: boolean;
};

export type MemberCouponListResponse = {
  memberCode: string;
  coupons: Coupon[];
};

export type PointsExchangeResponse = {
  exchangeId: string;
  memberCode: string;
  pointsUsed: number;
  availablePointsAfter: number;
  businessDate: string;
  coupon: Coupon;
};

export type PointsLotteryDrawResponse = {
  drawId: string;
  memberCode: string;
  activityCode: string;
  pointsCost: number;
  availablePointsAfter: number;
  prizeType: string;
  resultLabel: string;
  prizeCoupon?: Coupon | null;
  businessDate: string;
  createdAt: string;
};

export type BenefitPackageItem = {
  itemName: string;
  quantity: number;
  remark: string;
  sourceRowNumber?: number | null;
};

export type BenefitPackage = {
  packageCode: string;
  packageName: string;
  salesChannel: string;
  salePrice: number;
  status: string;
  sourceSheetName: string;
  sourceRowNumber?: number | null;
  items: BenefitPackageItem[];
};

export type BenefitPackagePurchaseResponse = {
  purchaseId: string;
  packageCode: string;
  packageName: string;
  salePrice: number;
  paymentAmount: number;
  memberCode: string;
  stationCode: string;
  checkoutTransactionNo: string;
  status: string;
  entitlementSnapshot: BenefitPackageItem[];
  purchasedAt: string;
};

export type PointsChangeRequest = {
  changeType: "ADD" | "SUBTRACT" | string;
  amount: number;
  reason?: string;
};

export type PointsChangeResponse = {
  memberCode: string;
  change: number;
  totalPoints: number;
  availablePoints: number;
  reason?: string | null;
};

export type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string | null;
};

export type BusinessClock = {
  businessDate: string;
  systemDate: string;
  overrideEnabled: boolean;
  updatedAt: string;
};

export type AuditLog = {
  auditId: string;
  actionType: string;
  entityType: string;
  entityId: string;
  beforeSnapshot?: unknown;
  afterSnapshot?: unknown;
  operatorId: string;
  operatorName: string;
  operatedAt: string;
  reason: string;
  createdAt: string;
};

export type ProductCatalogItem = {
  productCode: string;
  barcode?: string | null;
  productName: string;
  category?: string | null;
  unitPrice: number;
  inventoryQuantity: number;
};

export type PromotionRule = {
  ruleId: string;
  activityName: string;
  ruleType: string;
  priority: number;
  exclusiveGroup?: string | null;
  stackable: boolean;
  status: string;
  condition: {
    productCodes: string[];
    excludedCategories: string[];
    includedCategories?: string[];
    fuelTypes?: string[];
    daysOfMonth?: number[];
    startDate?: string | null;
    endDate?: string | null;
    minCartAmount: number;
    minFuelAmount: number;
    memberRequired: boolean;
    minInventoryQuantity: number;
    minProductQuantity?: number;
  };
  benefit: {
    type: string;
    fixedPrice: number;
    discountRate: number;
    amountOff: number;
    exchangePrice: number;
    exchangeQuantity: number;
    giftItemCode?: string | null;
    giftItemName?: string | null;
    giftItemQuantity: number;
    giftCouponName?: string | null;
    giftCouponAmount: number;
    giftCouponQuantity: number;
    giftCouponUseThreshold: number;
    giftCouponValidDays: number;
    bundleId?: string | null;
    bundleItems: { productCode: string; quantity: number }[];
    bundlePrice: number;
    giftItemOptions?: { productCode: string; name: string; quantity: number }[][];
    };
  version: string;
};

export type PromotionRuleDraft = {
  draftId: string;
  rule: PromotionRule;
  sourceImportId: string;
  sourceSheetName: string;
  sourceRowNumber: number;
  status: string;
  manualLocked: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
};

export type PromotionRuleVersion = {
  versionId: string;
  ruleId: string;
  status: string;
  changeReason: string;
};

export type PromotionRuleAuditLog = {
  auditId: string;
  ruleId: string;
  action: string;
  statusBefore?: string | null;
  statusAfter?: string | null;
  operatorId: string;
  changeReason: string;
  createdAt: string;
};

export type ImportBatch = {
  importId: { value: string };
  importType: string;
  sourceFile: string;
  insertedCount: number;
  updatedCount: number;
  skippedCount: number;
  invalidCount: number;
  warningCount: number;
  createdAt: string;
};

export type ImportErrorRow = {
  importId: { value: string };
  sheetName: string;
  rowNumber: number;
  columnName: string;
  rawValue: string;
  errorCode: string;
  rawValues: string[];
  errorMessage: string;
  severity: string;
};

export type InventoryAlert = {
  alertId: string;
  productCode: string;
  barcode?: string | null;
  productName: string;
  category?: string | null;
  currentQuantity: number;
  threshold: number;
  suggestedReplenishmentQuantity: number;
  relatedRuleId: string;
  relatedRuleType: string;
  severity: "LOW" | "CRITICAL" | "OUT_OF_STOCK" | "NO_STATION_STOCK";
  reason: string;
  status: "OPEN" | "REPLENISHMENT_CREATED" | "HANDLED";
  handledAt?: string | null;
  handledBy?: string | null;
  handleNote?: string | null;
  replenishmentListId?: string | null;
};

export type InventoryAlertHandleRequest = {
  operatorId?: string;
  note?: string;
};

export type ReplenishmentItem = {
  productCode: string;
  barcode?: string | null;
  productName: string;
  category?: string | null;
  currentQuantity: number;
  threshold: number;
  suggestedQuantity: number;
  relatedPromotion: string;
  reason: string;
};

export type ReplenishmentList = {
  listId: string;
  listName: string;
  createdAt: string;
  status: string;
  items: ReplenishmentItem[];
  totalItems: number;
  createdBy: string;
  updatedBy: string;
  updatedAt: string;
};
