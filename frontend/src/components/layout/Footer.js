import React from "react";
import { useHistory, useLocation } from "react-router-dom";
import {
  FaHome,
  FaUserInjured,
  FaFileInvoice,
  FaClipboardList,
  FaCog,
} from "react-icons/fa";

const quickLinks = [
  { label: "Home", route: "/home", icon: <FaHome /> },
  { label: "Patients", route: "/patients", icon: <FaUserInjured /> },
  { label: "Orders", route: "/orders", icon: <FaClipboardList /> },
  { label: "Reports", route: "/reports", icon: <FaFileInvoice /> },
  { label: "Settings", route: "/settings", icon: <FaCog /> },
];

function Footer() {
  const history = useHistory();
  const location = useLocation();

  return (
    <footer className="quick-footer">
      {quickLinks.map((item) => (
        <div
          key={item.route}
          className={
            location.pathname === item.route
              ? "footer-btn active"
              : "footer-btn"
          }
          onClick={() => history.push(item.route)}
        >
          {item.icon}
          <span>{item.label}</span>
        </div>
      ))}
    </footer>
  );
}

export default Footer;
