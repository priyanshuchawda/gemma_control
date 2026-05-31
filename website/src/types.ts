export type DownloadKind = "android" | "model" | "source";
export type DownloadStatus = "available" | "planned";

export interface DownloadItem {
  readonly id: string;
  readonly kind: DownloadKind;
  readonly title: string;
  readonly eyebrow: string;
  readonly description: string;
  readonly href: string;
  readonly fileName: string;
  readonly status: DownloadStatus;
  readonly primary: boolean;
  readonly meta: readonly string[];
  readonly secondaryHref?: string;
  readonly secondaryLabel?: string;
}
