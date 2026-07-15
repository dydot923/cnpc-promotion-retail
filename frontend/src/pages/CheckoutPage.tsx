import {
  CheckCircleOutlined,
  DeleteOutlined,
  GiftOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
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
import { calculateCheckout, confirmCheckout, fetchExchangeOffers } from "../api/checkout";
import { fetchProductByBarcode, searchProducts } from "../api/products";
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
  stationType: string;
  stationProvince?: string;
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

type CheckoutMode = "shop" | "fuel" | "coupon";

type ProductLookupResult = {
  query: string;
  products: ProductCatalogItem[];
  selected?: ProductCatalogItem;
};

type DemoCase = {
  key: string;
  label: string;
  cartItems: CartItem[];
  fuel: FuelForm;
  customer: CustomerForm;
  context?: Partial<TransactionContextForm>;
};

const demoCases: DemoCase[] = [
  {
    key: "fixed",
    label: "9.9 固定价",
    cartItems: [item("fixed-sku", "固定价商品", 1, 12, "零食")],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "member-001" }
  },
  {
    key: "amount",
    label: "满减",
    cartItems: [item("amount-sku", "满减商品", 1, 120, "家庭食品")],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "member-001" }
  },
  {
    key: "giftItem",
    label: "买赠",
    cartItems: [item("gift-buy-sku", "买赠商品", 1, 40, "便利店")],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "member-001" }
  },
  {
    key: "giftCoupon",
    label: "赠券",
    cartItems: [item("coupon-sku", "赠券商品", 1, 30, "便利店")],
    fuel: fuel(),
    customer: { member: true, memberLevel: "gold", memberCode: "member-001" }
  },
  {
    key: "bundle",
    label: "组合包",
    cartItems: [
      item("70536790", "玻璃水 2L", 1, 8, "车辅"),
      item("70545526", "格桑泉 500ml", 1, 3, "包装饮料")
    ],
    fuel: fuel("GASOLINE", "92", 220, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "member-001" }
  },
  {
    key: "exchange",
    label: "加油换购",
    cartItems: [item("70545523", "格桑泉 330ml", 4, 2.5, "包装饮料")],
    fuel: fuel("GASOLINE", "92", 220, 0),
    customer: { member: true, memberLevel: "gold", memberCode: "member-001" }
  },
  {
    key: "original",
    label: "无促销原价",
    cartItems: [item("no-promo-sku", "普通商品", 1, 18, "便利店")],
    fuel: fuel(),
    customer: { member: false, memberLevel: "", memberCode: "" }
  }
];

const stationTypeOptions = [
  { value: "gas_station", label: "加油站" },
  { value: "gas_cng", label: "CNG 站" },
  { value: "gas_lng", label: "LNG 站" }
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
  const [productForm] = Form.useForm<ProductForm>();
  const [fuelForm] = Form.useForm<FuelForm>();
  const [customerForm] = Form.useForm<CustomerForm>();
  const [contextForm] = Form.useForm<TransactionContextForm>();
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [barcode, setBarcode] = useState("");
  const [mode, setMode] = useState<CheckoutMode>("shop");
  const [selectedCandidateId, setSelectedCandidateId] = useState<string>();
  const [latestConfirmationId, setLatestConfirmationId] = useState<string>();
  const [api, contextHolder] = message.useMessage();
  const barcodeInputRef = useRef<InputRef>(null);
  const watchedFuelType = Form.useWatch("fuelType", fuelForm) as FuelType | undefined;
  const watchedFuelAmount = Form.useWatch("amount", fuelForm) as number | undefined;
  const watchedStationType = Form.useWatch("stationType", contextForm) as string | undefined;
  const watchedStationProvince = Form.useWatch("stationProvince", contextForm) as string | undefined;

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
      watchedStationProvince
    ],
    queryFn: () =>
      fetchExchangeOffers({
        fuelType: watchedFuelType || "GASOLINE",
        fuelAmount: Number(watchedFuelAmount || 0),
        businessDate: currentBusinessTime().businessDate,
        stationType: watchedStationType || "gas_station",
        stationProvince: watchedStationProvince || "新疆"
      }),
    enabled: mode === "fuel" && Boolean(watchedFuelType && watchedFuelType !== "NONE"),
    staleTime: 10_000
  });

  const result = checkoutMutation.data;
  const recommended = useMemo(
    () => result?.availableCandidates.find((candidate) => candidate.candidateId === result.recommendedCandidateId),
    [result]
  );
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

  useEffect(() => {
    barcodeInputRef.current?.focus();
  }, []);

  useEffect(() => {
    if (result) {
      setSelectedCandidateId(result.recommendedCandidateId || result.originalPriceFallback.candidateId);
      setLatestConfirmationId(undefined);
      confirmMutation.reset();
    }
  }, [result]);

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
    setCartItems(demo.cartItems);
    fuelForm.setFieldsValue(demo.fuel);
    customerForm.setFieldsValue(demo.customer);
    contextForm.setFieldsValue({
      ...defaultContext(),
      memberBirthMonth: demo.customer.member ? 7 : undefined,
      ...(demo.context || {})
    });
    setMode(demo.fuel.fuelType === "NONE" ? "shop" : "fuel");
    resetCalculation();
    barcodeInputRef.current?.focus();
  }

  function applyMode(nextMode: CheckoutMode) {
    setMode(nextMode);
    if (nextMode === "shop") {
      fuelForm.setFieldsValue(fuel());
      contextForm.setFieldsValue({
        couponId: undefined,
        couponName: undefined,
        couponFaceValue: undefined,
        couponMinSpendAmount: undefined,
        selectedCouponIds: undefined
      });
    }
    if (nextMode === "fuel") {
      fuelForm.setFieldsValue(fuel("GASOLINE", "92", 200, 0));
    }
    if (nextMode === "coupon") {
      customerForm.setFieldsValue({ member: true, memberLevel: "gold", memberCode: "" });
      contextForm.setFieldsValue({
        couponId: "coupon-demo-001",
        couponName: "会员抵扣券",
        couponFaceValue: 10,
        couponMinSpendAmount: 30,
        selectedCouponIds: "coupon-demo-001",
        couponStackable: false
      });
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
    if (cartItems.length === 0) {
      api.warning("购物车为空，无法计算促销");
      barcodeInputRef.current?.focus();
      return;
    }
    const fuelValues = await fuelForm.validateFields();
    const customerValues = await customerForm.validateFields();
    const contextValues = await contextForm.validateFields();
    const { businessDate, businessTime } = currentBusinessTime();
    const selectedCouponIds = resolveSelectedCouponIds(contextValues);
    const availableCoupons = buildAvailableCoupons(contextValues);
    const request: CheckoutCalculateRequest = {
      orderContext: {
        station: {
          stationId: "station-001",
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
        availableCoupons
      },
      transactionDate: businessDate,
      transactionTime: businessTime,
      stationType: contextValues.stationType || "gas_station",
      stationProvince: contextValues.stationProvince || "新疆",
      isMember: customerValues.member,
      memberLevel: customerValues.member ? customerValues.memberLevel || "gold" : null,
      memberCode: customerValues.member ? customerValues.memberCode || null : null,
      memberBirthMonth: customerValues.member ? contextValues.memberBirthMonth || null : null,
      fuelType: fuelValues.fuelType,
      fuelAmount: fuelValues.amount || 0,
      fuelVolume: fuelValues.volume || 0,
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
    customerForm.setFieldsValue({ member: true, memberLevel: "gold", memberCode: "member-001" });
    contextForm.setFieldsValue(defaultContext());
    resetCalculation();
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
                  disabled={cartItems.length === 0}
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

            {productLookupMutation.data?.products.length && !productLookupMutation.data.selected ? (
              <>
                <div className="product-result-summary">
                  <span>找到 {productLookupMutation.data.products.length} 个商品</span>
                  <span>选择一项加入，或继续输入缩小范围</span>
                </div>
                <div className="product-result-list">
                  {productLookupMutation.data.products.slice(0, 8).map((product) => (
                    <button key={product.productCode} className="product-result-button" type="button" onClick={() => selectProduct(product)}>
                      <span>
                        <strong>{product.productName}</strong>
                        <small>
                          编码 {product.productCode}
                          {product.barcode ? ` / 条码 ${product.barcode}` : ""}
                        </small>
                      </span>
                      <Price amount={product.unitPrice} size="small" />
                    </button>
                  ))}
                </div>
              </>
            ) : null}

            <div className="mode-actions">
              <Button className={mode === "shop" ? "mode-button active" : "mode-button"} onClick={() => applyMode("shop")}>
                便利店
              </Button>
              <Button className={mode === "fuel" ? "mode-button active" : "mode-button"} onClick={() => applyMode("fuel")}>
                加油换购
              </Button>
              <Button className={mode === "coupon" ? "mode-button active" : "mode-button"} onClick={() => applyMode("coupon")}>
                用券结算
              </Button>
            </div>

            {mode === "fuel" ? (
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
                  label: "快速验证样例",
                  children: (
                    <div className="demo-actions">
                      {demoCases.map((demo) => (
                        <Button key={demo.key} onClick={() => loadDemo(demo)}>
                          {demo.label}
                        </Button>
                      ))}
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
                initialValues={{ member: true, memberLevel: "gold", memberCode: "member-001" }}
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
                  label: "站点、优惠券和油品牌号",
                  forceRender: true,
                  children: (
                    <Form<TransactionContextForm> form={contextForm} layout="vertical" initialValues={defaultContext()}>
                      <div className="compact-field-grid context-fields">
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
                  <Price amount={cartTotal} size="medium" />
                </div>
                <div>
                  <span>下一步</span>
                  <strong>{cartItems.length ? "一键计算促销" : "扫码加入商品"}</strong>
                </div>
              </div>
            </section>
          ) : null}

          {result ? (
            <>
              <PromotionOutcome result={result} recommended={recommended} />

              <section className="panel">
                <Typography.Title level={3} className="section-title">
                  可用促销方案
                </Typography.Title>
                {result.availableCandidates.length === 0 ? (
                  <EmptyState description="没有可用促销" />
                ) : (
                  <div className="candidate-list">
                    {result.availableCandidates.map((candidate) => (
                      <CandidateCard
                        key={candidate.candidateId}
                        candidate={candidate}
                        selected={selectedCandidateId === candidate.candidateId}
                        recommended={candidate.candidateId === result.recommendedCandidateId}
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
  const hasPromotion = Boolean(recommended) && saved > 0;
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
          <strong>省 {formatMoney(saved)}</strong>
          <small>{savedRate > 0 ? `约 ${savedRate}%` : "无优惠"}</small>
        </div>
      </div>

      <div className="feedback-grid">
        <FeedbackTile label="原价" value={<Price amount={result.originalAmount} size="medium" variant="muted" strike={saved > 0} />} />
        <FeedbackTile label="可选活动" value={`${result.availableCandidates.length} 个`} />
        <FeedbackTile label="不可用活动" value={`${result.blockedPromotions.length} 个`} tone={result.blockedPromotions.length ? "warning" : "normal"} />
        <FeedbackTile label="库存/系统提醒" value={`${warnings} 条`} tone={warnings ? "warning" : "normal"} />
      </div>

      <div className="promotion-talk">
        <GiftOutlined />
        <span>
          {hasPromotion
            ? `已为顾客选择“${recommended?.title}”，少付 ${formatMoney(saved)}。`
            : "本单没有产生优惠，可按原价继续收款。"}
        </span>
      </div>

      {recommended ? (
        <div className="reward-strip">
          {renderGifts(recommended.gifts)}
          {renderCoupons(recommended.coupons)}
          {recommended.consumedCouponIds.length > 0 ? <Tag color="purple">核销券 {recommended.consumedCouponIds.join(", ")}</Tag> : null}
        </div>
      ) : null}
    </section>
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

  return (
    <section className="panel">
      <Typography.Title level={3} className="section-title">
        不可用促销
      </Typography.Title>
      {result.blockedPromotions.length === 0 ? (
        <EmptyState description="没有不可用促销" />
      ) : (
        <Collapse
          items={result.blockedPromotions.slice(0, 8).map((promotion) => ({
            key: `${promotion.ruleId}-${promotion.ruleVersionId || "unversioned"}`,
            label: `${promotion.title} / ${ruleTypeLabel(promotion.ruleType)}`,
            children: <BlockedReasons promotion={promotion} />
          }))}
        />
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

function item(productCode: string, name: string, quantity: number, unitPrice: number, category: string): CartItem {
  return {
    lineId: `line-${productCode}`,
    productCode,
    barcode: `barcode-${productCode}`,
    name,
    quantity,
    unitPrice,
    category,
    inventoryQuantity: 20
  };
}

function fuel(fuelType: FuelType = "NONE", fuelGrade = "", amount = 0, volume = 0): FuelForm {
  return { fuelType, fuelGrade, amount, volume };
}

function defaultContext(): TransactionContextForm {
  return {
    stationType: "gas_station",
    stationProvince: "新疆",
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
      validFrom: currentBusinessTime().businessDate,
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

function lineTotal(record: CartItem) {
  return Number(record.unitPrice || 0) * Number(record.quantity || 0);
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
