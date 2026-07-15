import { Empty } from "antd";
import type { ReactNode } from "react";

type EmptyStateProps = {
  description: string;
  action?: ReactNode;
};

export default function EmptyState({ description, action }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <Empty description={description}>{action}</Empty>
    </div>
  );
}
