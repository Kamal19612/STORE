import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import MainLayout from "./layouts/MainLayout";
import AdminLayout from "./layouts/AdminLayout";
import Home from "./pages/public/Home";
import Checkout from "./pages/public/Checkout";
import AdminLogin from "./pages/admin/AdminLogin";
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminProductList from "./pages/admin/products/AdminProductList";
import AdminProductForm from "./pages/admin/products/AdminProductForm";
import AdminOrderList from "./pages/admin/orders/AdminOrderList";
import AdminOrderDetail from "./pages/admin/orders/AdminOrderDetail";
import AdminSlider from "./pages/admin/slider/AdminSlider";
import AdminUserList from "./pages/admin/users/AdminUserList";
import AdminUserForm from "./pages/admin/users/AdminUserForm";
import PrivateRoute from "./components/PrivateRoute";

function App() {
  return (
    <Router>
      <Routes>
        {/* Routes Publiques */}
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Home />} />
          <Route path="products" element={<Home />} />{" "}
          {/* Temporairement la même page */}
          <Route
            path="about"
            element={
              <div className="p-20 text-center">
                Page À Propos en construction
              </div>
            }
          />
          <Route
            path="cart"
            element={
              <div className="p-20 text-center">
                Page Panier en construction
              </div>
            }
          />
          <Route path="checkout" element={<Checkout />} />
        </Route>

        {/* Route de connexion admin (NON protégée) */}
        <Route path="/admin/login" element={<AdminLogin />} />

        {/* Routes Admin (PROTÉGÉES) */}
        <Route
          path="/admin"
          element={
            <PrivateRoute>
              <AdminLayout />
            </PrivateRoute>
          }
        >
          <Route index element={<AdminDashboard />} />
          <Route path="dashboard" element={<AdminDashboard />} />

          {/* Placeholders pour les futures pages admin */}
          <Route path="orders" element={<AdminOrderList />} />
          <Route path="orders/:id" element={<AdminOrderDetail />} />
          <Route path="products" element={<AdminProductList />} />
          <Route path="products/new" element={<AdminProductForm />} />
          <Route path="products/edit/:id" element={<AdminProductForm />} />
          <Route path="slider" element={<AdminSlider />} />
          <Route
            path="users"
            element={
              <PrivateRoute allowedRoles={["SUPER_ADMIN"]}>
                <AdminUserList />
              </PrivateRoute>
            }
          />
          <Route
            path="users/new"
            element={
              <PrivateRoute allowedRoles={["SUPER_ADMIN"]}>
                <AdminUserForm />
              </PrivateRoute>
            }
          />
          <Route
            path="users/edit/:id"
            element={
              <PrivateRoute allowedRoles={["SUPER_ADMIN"]}>
                <AdminUserForm />
              </PrivateRoute>
            }
          />
        </Route>
      </Routes>

      <ToastContainer position="bottom-right" />
    </Router>
  );
}

export default App;
