import { useEffect, useState } from "react";

type BackendStatus = {
  connected: boolean;
  latency: number | null;
};

export default function useBackendStatus(): BackendStatus {
  const [status, setStatus] = useState<BackendStatus>({ connected: false, latency: null });

  useEffect(() => {
    let disposed = false;

    async function ping() {
      const startedAt = performance.now();
      try {
        const connected = await pingBackend();
        if (!disposed) {
          setStatus({
            connected,
            latency: connected ? Math.max(1, Math.round(performance.now() - startedAt)) : null
          });
        }
      } catch {
        if (!disposed) {
          setStatus({ connected: false, latency: null });
        }
      }
    }

    void ping();
    const timer = window.setInterval(ping, 30_000);
    return () => {
      disposed = true;
      window.clearInterval(timer);
    };
  }, []);

  return status;
}

async function pingBackend() {
  try {
    const response = await fetch("/api/checkout/capabilities", {
      cache: "no-store",
      headers: { Accept: "application/json" }
    });
    if (!response.ok) {
      return false;
    }
    const payload = (await response.json()) as {
      success?: boolean;
      data?: {
        service?: string;
        apiVersion?: string;
        calculate?: boolean;
        confirm?: boolean;
      };
    };
    return (
      payload.success === true &&
      payload.data?.service === "cnpc-promotion-retail" &&
      payload.data.apiVersion === "checkout-v2" &&
      payload.data.calculate === true &&
      payload.data.confirm === true
    );
  } catch {
    return false;
  }
}
