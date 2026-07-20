/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        avo: {
          blue: '#0076C8',
          darkblue: '#155A8A',
          orange: '#F58220',
          gray: '#666666',
          lightgray: '#D9D9D9',
          bg: '#F4F6F9',
        }
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
