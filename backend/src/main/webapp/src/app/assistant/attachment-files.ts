/**
 * Photos, sketches and PDFs a Member attaches to a message, within the
 * limits the server checks again: ten files and 25 MB per message.
 */
export const MAX_FILES = 10;
export const MAX_MESSAGE_BYTES = 25 * 1024 * 1024;
/** Photos are sent at most this wide or tall; the model looks at them smaller anyway. */
export const MAX_IMAGE_EDGE = 2048;
const MAX_KEPT_IMAGE_BYTES = 4 * 1024 * 1024;

export const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf"];

/** Why these files cannot be sent, or null. */
export function filesProblem(files: File[]): string | null {
  const refused = files.find(file => !ACCEPTED_TYPES.includes(file.type));
  if (refused) {
    return $localize`:@@attachments.type:${refused.name}:name: n'est ni une photo (JPEG, PNG, WebP, GIF) ni un PDF.`;
  }
  if (files.length > MAX_FILES) {
    return $localize`:@@attachments.count:Au plus ${MAX_FILES}:max: fichiers par message.`;
  }
  if (files.reduce((total, file) => total + file.size, 0) > MAX_MESSAGE_BYTES) {
    return $localize`:@@attachments.size:Au plus 25 Mo de fichiers par message.`;
  }
  return null;
}

/**
 * A photo larger than the model needs, shrunk to JPEG on a white background
 * (sketches drawn on transparency stay readable); anything else as it is.
 */
export async function prepare(file: File): Promise<File> {
  if (!file.type.startsWith("image/") || file.type === "image/gif") {
    return file;
  }
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, MAX_IMAGE_EDGE / Math.max(bitmap.width, bitmap.height));
  if (scale === 1 && file.size <= MAX_KEPT_IMAGE_BYTES) {
    bitmap.close();
    return file;
  }
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  const context = canvas.getContext("2d")!;
  context.fillStyle = "#ffffff";
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  const blob = await new Promise<Blob>((resolve, reject) =>
    canvas.toBlob(result => result ? resolve(result) : reject(new Error("image")), "image/jpeg", 0.85));
  return new File([blob], file.name.replace(/\.[^.]+$/, "") + ".jpg", { type: "image/jpeg" });
}

/** A size for people: 340 Ko, 2,4 Mo. */
export function sizeLabel(bytes: number, locale: string): string {
  const format = (value: number) => value.toLocaleString(locale, { maximumFractionDigits: 1 });
  return bytes < 1024 * 1024
    ? $localize`:@@attachments.kilobytes:${format(Math.max(1, Math.round(bytes / 1024)))}:size: Ko`
    : $localize`:@@attachments.megabytes:${format(bytes / 1024 / 1024)}:size: Mo`;
}
