<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
  final String redirectURL = "/api/OpenELIS-Global/";
  response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
  response.sendRedirect(redirectURL);
%>
