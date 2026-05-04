import { useMemo, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

const fallbackSvg =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='600' height='600'%3E%3Crect width='600' height='600' fill='%23f3f4f6'/%3E%3C/svg%3E";

const ProductImageCarousel = ({ mainImage, secondaryImages, alt }) => {
  const images = useMemo(() => {
    const list = [mainImage, ...(secondaryImages || [])]
      .filter(Boolean)
      .map((s) => String(s).trim())
      .filter(Boolean);

    // unique en conservant l'ordre
    const seen = new Set();
    return list.filter((u) => (seen.has(u) ? false : (seen.add(u), true)));
  }, [mainImage, secondaryImages]);

  const [index, setIndex] = useState(0);

  const active = images[index] || mainImage || fallbackSvg;
  const hasMany = images.length > 1;

  const prev = () => setIndex((i) => (i - 1 + images.length) % images.length);
  const next = () => setIndex((i) => (i + 1) % images.length);

  return (
    <div className="bg-white rounded-lg shadow-lg overflow-hidden">
      <div className="relative aspect-square w-full bg-gray-100 flex items-center justify-center">
        <img src={active} alt={alt} className="w-full h-full object-cover" />

        {hasMany && (
          <>
            <button
              type="button"
              onClick={prev}
              className="absolute left-3 top-1/2 -translate-y-1/2 p-2 rounded-full bg-white/80 hover:bg-white shadow transition-colors"
              aria-label="Image précédente"
            >
              <ChevronLeft className="h-5 w-5 text-gray-800" />
            </button>
            <button
              type="button"
              onClick={next}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-2 rounded-full bg-white/80 hover:bg-white shadow transition-colors"
              aria-label="Image suivante"
            >
              <ChevronRight className="h-5 w-5 text-gray-800" />
            </button>
          </>
        )}
      </div>

      {hasMany && (
        <div className="p-3">
          <div className="flex gap-2 overflow-x-auto">
            {images.map((url, i) => (
              <button
                key={`${url}:${i}`}
                type="button"
                onClick={() => setIndex(i)}
                className={`shrink-0 rounded-lg overflow-hidden border-2 transition-colors ${
                  i === index ? "border-primary" : "border-transparent"
                }`}
                aria-label={`Voir image ${i + 1}`}
              >
                <img src={url} alt="" className="h-16 w-16 object-cover bg-gray-100" />
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductImageCarousel;

