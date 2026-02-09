/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "#e5a543",
          dark: "#d49231",
        },
        secondary: {
          DEFAULT: "#1a1a1a",
          light: "#2d2d2d",
        },
      },
      keyframes: {
        bounce: {
          "0%, 100%": { transform: "scale(1)" },
          "50%": { transform: "scale(1.2)" },
        },
        slideIn: {
          from: { transform: "translateX(400px)", opacity: "0" },
          to: { transform: "translateX(0)", opacity: "1" },
        },
        blink: {
          "0%, 100%": { opacity: "1" },
          "50%": { opacity: "0.4" },
        },
      },
      animation: {
        "bounce-subtle": "bounce 0.5s ease-in-out",
        "slide-in": "slideIn 0.3s ease-out forwards",
        "blink-slow": "blink 1.5s ease-in-out infinite",
      },
    },
  },
  plugins: [],
};
