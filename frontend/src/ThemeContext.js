import React, { createContext, useState, useEffect, useContext } from "react";

// Create Theme Context
export const ThemeContext = createContext({
  theme: "white",
  toggleTheme: () => {},
});

// Theme Provider Component
export const ThemeProvider = ({ children }) => {
  // Initialize theme from localStorage or default to 'white' (light mode)
  const [theme, setTheme] = useState(() => {
    const savedTheme = localStorage.getItem("oe-theme");
    return savedTheme || "white";
  });

  // Toggle between light and dark themes
  const toggleTheme = () => {
    setTheme((prevTheme) => {
      const newTheme = prevTheme === "white" ? "g100" : "white";
      localStorage.setItem("oe-theme", newTheme);
      return newTheme;
    });
  };

  // Update document element with theme class for global styling
  useEffect(() => {
    document.documentElement.setAttribute("data-carbon-theme", theme);
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

// Custom hook to use theme context
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme must be used within a ThemeProvider");
  }
  return context;
};
