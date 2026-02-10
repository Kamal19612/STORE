import { useState, useEffect } from "react";
import { useNavigate, useParams, useLocation, Link } from "react-router-dom";
import { ArrowLeft, Save, User, Lock, Mail, Shield } from "lucide-react";
import { toast } from "react-toastify";
import adminUserService from "../../../services/adminUserService";

const AdminUserForm = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const location = useLocation();
  const isEditMode = !!id;

  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "", // Only sent if changed
    role: "MANAGER",
    active: true,
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEditMode) {
      // Use state if available (from list) or fetch
      if (location.state?.user) {
        const u = location.state.user;
        setFormData({
          username: u.username,
          email: u.email,
          role: u.role,
          active: u.active,
          password: "", // Don't fill password
        });
      }
      // Ideally should fetch fresh data if relying on URL
    }
  }, [isEditMode, location.state]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isEditMode) {
        await adminUserService.updateUser(id, formData);
        toast.success("Utilisateur mis à jour");
      } else {
        await adminUserService.createUser(formData);
        toast.success("Utilisateur créé");
      }
      navigate("/admin/users");
    } catch (error) {
      console.error(error);
      const msg =
        error.response?.data?.message ||
        error.message ||
        "Erreur enregistrement";
      toast.error(
        msg.includes("403")
          ? "Permission refusée (Super Admin requis)"
          : "Erreur: " + msg,
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <div className="flex items-center gap-4 mb-8">
        <Link
          to="/admin/users"
          className="p-2 bg-white rounded-full border border-gray-200 hover:bg-gray-50 transition-colors"
        >
          <ArrowLeft className="h-5 w-5 text-gray-500" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            {isEditMode ? "Modifier Utilisateur" : "Nouvel Utilisateur"}
          </h1>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-8">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Username */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Nom d'utilisateur
              </label>
              <div className="relative">
                <User className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
                <input
                  type="text"
                  required
                  value={formData.username}
                  onChange={(e) =>
                    setFormData({ ...formData, username: e.target.value })
                  }
                  className="pl-10 w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary"
                />
              </div>
            </div>

            {/* Email */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Email
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
                <input
                  type="email"
                  required
                  value={formData.email}
                  onChange={(e) =>
                    setFormData({ ...formData, email: e.target.value })
                  }
                  className="pl-10 w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Password */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {isEditMode
                  ? "Mot de passe (Laisser vide pour ne pas changer)"
                  : "Mot de passe"}
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
                <input
                  type="password"
                  required={!isEditMode}
                  value={formData.password}
                  onChange={(e) =>
                    setFormData({ ...formData, password: e.target.value })
                  }
                  className="pl-10 w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary"
                />
              </div>
            </div>

            {/* Role */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Rôle
              </label>
              <div className="relative">
                <Shield className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
                <select
                  value={formData.role}
                  onChange={(e) =>
                    setFormData({ ...formData, role: e.target.value })
                  }
                  className="pl-10 w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary"
                >
                  <option value="MANAGER">Manager (Produits/Commandes)</option>
                  <option value="DELIVERY_AGENT">Livreur</option>
                  <option value="ADMIN">Admin</option>
                  <option value="SUPER_ADMIN">Super Admin (Accès Total)</option>
                </select>
              </div>
            </div>
          </div>

          {/* Active Status */}
          <div className="flex items-center gap-3 pt-2">
            <input
              type="checkbox"
              id="active"
              checked={formData.active}
              onChange={(e) =>
                setFormData({ ...formData, active: e.target.checked })
              }
              className="h-5 w-5 text-primary focus:ring-primary border-gray-300 rounded"
            />
            <label
              htmlFor="active"
              className="text-sm font-medium text-gray-700"
            >
              Compte Actif
            </label>
          </div>

          <div className="pt-6 border-t border-gray-100 flex justify-end">
            <button
              type="submit"
              disabled={loading}
              className="bg-primary text-white px-6 py-2.5 rounded-lg hover:bg-primary/90 font-medium flex items-center gap-2 disabled:opacity-70"
            >
              <Save className="h-5 w-5" />
              {loading ? "Enregistrement..." : "Enregistrer"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AdminUserForm;
