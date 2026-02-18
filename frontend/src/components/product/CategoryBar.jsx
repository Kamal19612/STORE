import { useRef } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

const CategoryBar = ({
  selectedCategory,
  onSelectCategory,
  categories = [],
}) => {
  const scrollRef = useRef(null);

  // Construire la liste avec "Tous" + catégories dynamiques
  const allCategories = ["Tous", ...categories];

  const scroll = (direction) => {
    if (scrollRef.current) {
      const { current } = scrollRef;
      const scrollAmount = 200;
      if (direction === "left") {
        current.scrollBy({ left: -scrollAmount, behavior: "smooth" });
      } else {
        current.scrollBy({ left: scrollAmount, behavior: "smooth" });
      }
    }
  };

  return (
    <div className="relative w-full bg-white shadow-sm border-b border-gray-100 py-2 lg:hidden flex items-center overflow-hidden">
      <div className="flex items-center w-full px-4">
        {/* Fixed Label - Matches PHP .category-btn-label */}
        <div className="flex-shrink-0 flex items-center bg-primary text-secondary px-4 py-2 rounded-full font-bold text-sm mr-3">
          <span className="mr-2">📂</span>
          <span>Catégorie</span>
        </div>

        {/* Categories Scroll - Matches PHP .categories-scroll */}
        <div
          ref={scrollRef}
          className="flex-1 flex gap-4 overflow-x-auto no-scrollbar scroll-smooth items-center"
        >
          {allCategories.map((cat) => (
            <button
              key={cat}
              onClick={() => onSelectCategory(cat)}
              className={`whitespace-nowrap text-sm font-semibold transition-colors duration-200 flex-shrink-0 ${
                selectedCategory === cat
                  ? "text-primary border-b-2 border-primary"
                  : "text-secondary hover:text-primary"
              }`}
            >
              {cat.toUpperCase()}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default CategoryBar;
