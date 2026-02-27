import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { Suspense, lazy } from "react";

// Layouts (Eager load critical layouts)
import MainLayout from "./layouts/MainLayout";
import AdminLayout from "./layouts/AdminLayout";
import DeliveryLayout from "./layouts/DeliveryLayout";
import PrivateRoute from "./components/PrivateRoute";

// Components
import LoadingSpinner from "./components/LoadingSpinner"; // Create this if missing, or use inline

// Pages - Code Splitting (Lazy Load)
const Home = lazy(() => import("./pages/public/Home"));
const Checkout = lazy(() => import("./pages/public/Checkout"));
const Login = lazy(() => import("./pages/Login"));

// Admin Pages
const AdminLogin = lazy(() => import("./pages/admin/AdminLogin"));
const AdminDashboard = lazy(() => import("./pages/admin/AdminDashboard"));
const AdminProductList = lazy(
  () => import("./pages/admin/products/AdminProductList"),
);
const AdminProductForm = lazy(
  () => import("./pages/admin/products/AdminProductForm"),
);
const AdminOrderList = lazy(
  () => import("./pages/admin/orders/AdminOrderList"),
);
const AdminOrderDetail = lazy(
  () => import("./pages/admin/orders/AdminOrderDetail"),
);
const AdminSlider = lazy(() => import("./pages/admin/slider/AdminSlider"));
const AdminUserList = lazy(() => import("./pages/admin/users/AdminUserList"));
const AdminUserForm = lazy(() => import("./pages/admin/users/AdminUserForm"));
const AdminSettings = lazy(() => import("./pages/admin/AdminSettings"));
const OrdersDiagnostic = lazy(() => import("./pages/admin/OrdersDiagnostic"));

// Delivery Pages
const DeliveryDashboard = lazy(
  () => import("./pages/delivery/DeliveryDashboard"),
);

// Fallback Loading Component
const PageLoader = () => (
  <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-[#1c191a]">
    <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[#f5ad41]"></div>
  </div>
);

import { ThemeProvider } from "./context/ThemeContext";

function App() {
  return (
    <Router>
      <ThemeProvider>
        <Suspense fallback={<PageLoader />}>
          <Routes>
            {/* Routes Publiques */}
            <Route path="/" element={<MainLayout />}>
              <Route index element={<Home />} />
              <Route path="products" element={<Home />} />
              <Route
                path="about"
                element={
                  <div className="p-20 text-center dark:text-white">
                    Page À Propos en construction
                  </div>
                }
              />
              <Route
                path="cart"
                element={
                  <div className="p-20 text-center dark:text-white">
                    Page Panier en construction
                  </div>
                }
              />
              <Route path="checkout" element={<Checkout />} />
            </Route>

            {/* Route de connexion principale */}
            <Route path="/login" element={<Login />} />

            {/* Route de connexion admin (NON protégée) */}
            <Route path="/admin/login" element={<AdminLogin />} />

            {/* Routes Admin (PROTÉGÉES) */}
            <Route
              path="/admin"
              element={
                <PrivateRoute
                  allowedRoles={["ADMIN", "SUPER_ADMIN", "MANAGER"]}
                >
                  <AdminLayout />
                </PrivateRoute>
              }
            >
              <Route index element={<AdminDashboard />} />
              <Route path="dashboard" element={<AdminDashboard />} />

              {/* Placeholders pour les futures pages admin */}
              <Route path="diagnostic" element={<OrdersDiagnostic />} />
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
              <Route path="settings" element={<AdminSettings />} />
            </Route>

            {/* Routes Livreur (PROTÉGÉES) */}
            <Route
              path="/delivery"
              element={
                <PrivateRoute
                  allowedRoles={["DELIVERY_AGENT", "ADMIN", "SUPER_ADMIN"]}
                >
                  <DeliveryLayout />
                </PrivateRoute>
              }
            >
              <Route index element={<DeliveryDashboard />} />
              <Route path="dashboard" element={<DeliveryDashboard />} />
            </Route>
          </Routes>
        </Suspense>

        <ToastContainer
          position="top-right"
          autoClose={4000}
          hideProgressBar={false}
          newestOnTop={false}
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
          theme="colored"
          style={{ top: "100px", minWidth: "300px" }}
        />
      </ThemeProvider>
    </Router>
  );
}

export default App;
