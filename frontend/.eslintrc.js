module.exports = {
  plugins: ["react", "react-hooks", "prettier"],
  extends: [
    "prettier",
    "eslint:recommended",
    "plugin:react/recommended",
    "plugin:react-hooks/recommended",
  ],
  parserOptions: {
    ecmaVersion: 12,
    sourceType: "module",
    ecmaFeatures: {
      jsx: true,
    },
  },
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  rules: {
    "react/prop-types": "off",
    "react/react-in-jsx-scope": "off",
    "react/no-unescaped-entities": "warn",
    "react-hooks/exhaustive-deps": "off",
    "no-unused-vars": "warn",
    "@typescript-eslint/no-empty-function": "warn",
    "prettier/prettier": ["warn"],
  },
  overrides: [
    {
      files: ["*.ts", "*.tsx"],
      parser: "@typescript-eslint/parser",
      plugins: ["@typescript-eslint"],
      extends: [
        "plugin:react/recommended",
        "plugin:@typescript-eslint/recommended",
      ],
      parserOptions: {
        ecmaversion: 2018,
        sourceType: "module",
        ecmaFeatures: {
          jsx: true,
        },
      },
      settings: {
        react: {
          version: "detect",
        },
      },

      /**
       * Typescript Rules
       */
      rules: {
        "react/prop-types": "warn",
        "react/react-in-jsx-scope": "off",
        "@typescript-eslint/no-empty-function": "warn",
        "@typescript-eslint/no-empty-interface": "warn",
        "no-case-declarations": "off",
        "react/display-name": "off",
      },
    },
  ],
};
