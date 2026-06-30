/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}", "./public/index.html"],
  theme: {
    extend: {
      colors: {
        carbon: {
          "openelis-bg-blue": "#295785",
          "openelis-bg-white": "#ffffff",
        },
        fontFamily: {
          sans: ["IBM Plex Sans", "sans-serif"],
        },
      },
    },
  },
  plugins: [],
};
