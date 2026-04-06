/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./app/**/*.{ts,tsx}', './src/**/*.{ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        kkiri: {
          bg: '#000000',
          surface: '#1C1C1E',
          card: '#2C2C2E',
          primary: '#FF3B30',
          text: '#FFFFFF',
          muted: '#8E8E93',
          border: '#3A3A3C',
        },
      },
    },
  },
  plugins: [],
};
