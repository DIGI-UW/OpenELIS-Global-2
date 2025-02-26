package org.openelisglobal.metricservice.action;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;

public class MetricServicesServlet extends HttpServlet {

    private static final long serialVersionUID = 947483610606055973L;

    /** Constructor of the object. */
    public MetricServicesServlet() {
        super();
    }

    /**
     * Destruction of the servlet. <br>
     */
    public void destroy() {
        super.destroy(); // Just puts "destroy" string in log
        // Put your code here
    }

    /**
     * The doGet method of the servlet. <br>
     * This method is called when a form has its tag value method equals to get.
     *
     * @param request  the request send by the client to the server
     * @param response the response send by the server to the client
     * @throws ServletException if an error occurred
     * @throws IOException      if an error occurred
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String permGen = getAvailablePermGen();
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();

        out.println("<?xml version = \"1.0\" encoding = \"utf-8\"?>");
        out.println("<metrics>");
        out.println("<permgen>");
        out.println(permGen);
        out.println("</permgen>");
        out.println("</metrics>");
        out.flush();
        out.close();
    }

    private String getAvailablePermGen() {

        List<MemoryPoolMXBean> memoryList = ManagementFactory.getMemoryPoolMXBeans();

        for (MemoryPoolMXBean memory : memoryList) {
            if (memory.getName().contains("Perm Gen")) {
                return String.valueOf(
                        (int) (((double) (memory.getUsage().getMax() - memory.getUsage().getUsed())) / 1024.0));
            }
        }
        return "0";
    }

    /**
     * The doPost method of the servlet. <br>
     * This method is called when a form has its tag value method equals to post.
     *
     * @param request  the request send by the client to the server
     * @param response the response send by the server to the client
     * @throws ServletException if an error occurred
     * @throws IOException      if an error occurred
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.close();
    }

    /**
     * Initialization of the servlet. <br>
     *
     * @throws ServletException if an error occurs
     */
    public void init() throws ServletException {
        // Put your code here
    }
}
