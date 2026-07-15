import type { ThemeConfig } from "antd";

export const brandColors = {
  red: "#D71920",
  blue: "#003F88",
  yellow: "#FFB81C",
  deepBlue: "#002A5C",
  lightBlue: "#E8F0FA",
  success: "#52C41A",
  warning: "#FA8C16",
  error: "#F5222D",
  gray: "#8C8C8C",
  pageBg: "#F5F7FA"
};

export const cnpcTheme: ThemeConfig = {
  token: {
    colorPrimary: brandColors.red,
    colorInfo: brandColors.blue,
    colorSuccess: brandColors.success,
    colorWarning: brandColors.warning,
    colorError: brandColors.error,
    colorLink: brandColors.blue,
    borderRadius: 6,
    fontSize: 15,
    fontSizeHeading1: 24,
    fontSizeHeading2: 20,
    fontSizeHeading3: 18,
    controlHeight: 40,
    controlHeightLG: 48,
    fontFamily: "-apple-system, BlinkMacSystemFont, Segoe UI, Microsoft YaHei, sans-serif"
  },
  components: {
    Layout: {
      siderBg: brandColors.blue,
      headerBg: "#FFFFFF",
      headerHeight: 60,
      footerBg: brandColors.deepBlue
    },
    Menu: {
      darkItemBg: brandColors.blue,
      darkItemSelectedBg: brandColors.deepBlue,
      darkItemColor: "#B0C4DE",
      darkItemSelectedColor: brandColors.yellow,
      darkItemHoverBg: "#004B8D"
    },
    Table: {
      headerBg: brandColors.blue,
      headerColor: "#FFFFFF",
      rowHoverBg: brandColors.lightBlue
    },
    Card: {
      headerBg: brandColors.pageBg,
      borderRadiusLG: 8
    },
    Button: {
      primaryShadow: "0 2px 4px rgba(215, 25, 32, 0.3)"
    }
  }
};
