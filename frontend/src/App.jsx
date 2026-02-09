import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import MainLayout from "./layouts/MainLayout";
import AdminLayout from "./layouts/AdminLayout";
import Home from "./pages/public/Home";
import Checkout from "./pages/public/Checkout";
import AdminLogin from "./pages/admin/AdminLogin";
import AdminDashboard from "./pages/admin/AdminDashboard";
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
          <Route
            path="orders"
            element={
              <div className="text-center py-20">
                <h2 className="text-2xl font-bold text-gray-800">
                  Page Commandes en construction
                </h2>
              </div>
            }
          />
          <Route
            path="orders/:id"
            element={
              <div className="text-center py-20">
                <h2 className="text-2xl font-bold text-gray-800">
                  Détail Commande en construction
                </h2>
              </div>
            }
          />
          <Route
            path="products"
            element={
              <div className="text-center py-20">
                <h2 className="text-2xl font-bold text-gray-800">
                  Gestion Produits en construction
                </h2>
              </div>
            }
          />
          <Route
            path="slider"
            element={
              <div className="text-center py-20">
                <h2 className="text-2xl font-bold text-gray-800">
                  Gestion Carrousel en construction
                </h2>
              </div>
            }
          />
          <Route
            path="users"
            element={
              <PrivateRoute allowedRoles={["SUPER_ADMIN"]}>
                <div className="text-center py-20">
                  <h2 className="text-2xl font-bold text-gray-800">
                    Gestion Utilisateurs en construction
                  </h2>
                  <p className="text-gray-600 mt-2">
                    Réservé aux SUPER_ADMIN uniquement
                  </p>
                </div>
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
