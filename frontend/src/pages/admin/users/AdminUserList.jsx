import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Plus, Edit, Trash2 } from "lucide-react";
import { toast } from "react-toastify";
import adminUserService from "../../../services/adminUserService";

const AdminUserList = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchUsers = async () => {
    try {
      const data = await adminUserService.getAllUsers();
      setUsers(data);
    } catch (error) {
      console.error(error);
      toast.error("Erreur chargement utilisateurs");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleDelete = async (id) => {
    if (window.confirm("Supprimer cet utilisateur ?")) {
      try {
        await adminUserService.deleteUser(id);
        toast.success("Utilisateur supprimé");
        fetchUsers();
      } catch (error) {
        toast.error("Erreur suppression (Permission insuffisante ?)");
      }
    }
  };

  const getRoleBadge = (role) => {
    const colors = {
      SUPER_ADMIN:
        "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300 border border-red-100 dark:border-red-900/50",
      ADMIN:
        "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300 border border-blue-100 dark:border-blue-900/50",
      MANAGER:
        "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300 border border-green-100 dark:border-green-900/50",
      DELIVERY_AGENT:
        "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300 border border-purple-100 dark:border-purple-900/50",
      CUSTOMER:
        "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300 border border-gray-100 dark:border-gray-600",
    };
    return (
      colors[role] ||
      "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300 border border-gray-100 dark:border-gray-600"
    );
  };

  return (
    <div className="p-3 sm:p-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-5 sm:mb-8">
        <div>
          <h1 className="text-lg sm:text-2xl font-bold text-gray-800 dark:text-white">
            Gestion Utilisateurs
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Gérer les accès au système
          </p>
        </div>
        <Link
          to="/admin/users/new"
          className="self-start sm:self-auto bg-primary text-white px-4 py-2 rounded-lg hover:bg-primary/90 flex items-center gap-2 shadow-lg shadow-primary/25 text-sm font-medium"
        >
          <Plus className="h-4 w-4" /> Nouvel Utilisateur
        </Link>
      </div>

      {/* Mobile cards */}
      <div className="lg:hidden space-y-3">
        {loading ? (
          <p className="text-center text-gray-500 dark:text-gray-400 py-8">Chargement...</p>
        ) : (
          users.map((user) => (
            <div key={user.id} className="bg-white dark:bg-[#242021] rounded-xl border border-gray-100 dark:border-white/10 p-4 shadow-sm transition-colors">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-10 h-10 rounded-full bg-gray-100 dark:bg-white/10 flex items-center justify-center font-bold text-gray-600 dark:text-gray-300 shrink-0">
                    {user.username.charAt(0).toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <p className="font-semibold text-gray-900 dark:text-white truncate">{user.username}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{user.email}</p>
                  </div>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  <Link
                    to={`/admin/users/edit/${user.id}`}
                    state={{ user }}
                    className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 dark:text-gray-400 dark:hover:text-blue-400 dark:hover:bg-blue-900/20 rounded-lg transition-colors"
                  >
                    <Edit className="h-4 w-4" />
                  </Link>
                  <button
                    onClick={() => handleDelete(user.id)}
                    className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 dark:text-gray-400 dark:hover:text-red-400 dark:hover:bg-red-900/20 rounded-lg transition-colors"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
              <div className="flex items-center gap-2 mt-3 flex-wrap">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getRoleBadge(user.role)}`}>
                  {user.role}
                </span>
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${user.active ? "bg-green-50 text-green-700 dark:bg-green-900/30 dark:text-green-300 border border-green-200 dark:border-green-900/50" : "bg-red-50 text-red-700 dark:bg-red-900/30 dark:text-red-300 border border-red-200 dark:border-red-900/50"}`}>
                  {user.active ? "Actif" : "Inactif"}
                </span>
                {user.createdAt && (
                  <span className="text-xs text-gray-400 dark:text-gray-500">
                    {new Date(user.createdAt).toLocaleDateString()}
                  </span>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Desktop table */}
      <div className="hidden lg:block bg-white dark:bg-[#242021] rounded-xl shadow-sm border border-gray-100 dark:border-white/10 overflow-hidden transition-colors">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead className="bg-gray-50 dark:bg-[#1c191a] border-b border-gray-200 dark:border-white/10">
              <tr>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Utilisateur</th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Email</th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Rôle</th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Statut</th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 dark:divide-white/5">
              {loading ? (
                <tr>
                  <td colSpan="5" className="px-6 py-10 text-center text-gray-500 dark:text-gray-400">Chargement...</td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user.id} className="hover:bg-gray-50 dark:hover:bg-white/5 transition-colors">
                    <td className="px-6 py-4 font-medium text-gray-900 dark:text-white">
                      {user.username}
                      {user.createdAt && (
                        <div className="text-xs text-gray-400 dark:text-gray-500">
                          Créé le {new Date(user.createdAt).toLocaleDateString()}
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 text-gray-600 dark:text-gray-300">{user.email}</td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getRoleBadge(user.role)}`}>
                        {user.role}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${user.active ? "bg-green-50 text-green-700 dark:bg-green-900/30 dark:text-green-300 border border-green-200 dark:border-green-900/50" : "bg-red-50 text-red-700 dark:bg-red-900/30 dark:text-red-300 border border-red-200 dark:border-red-900/50"}`}>
                        {user.active ? "Actif" : "Inactif"}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/admin/users/edit/${user.id}`}
                          state={{ user }}
                          className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 dark:text-gray-400 dark:hover:text-blue-400 dark:hover:bg-blue-900/20 rounded-lg transition-colors"
                          title="Modifier"
                        >
                          <Edit className="h-5 w-5" />
                        </Link>
                        <button
                          onClick={() => handleDelete(user.id)}
                          className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 dark:text-gray-400 dark:hover:text-red-400 dark:hover:bg-red-900/20 rounded-lg transition-colors"
                          title="Supprimer"
                        >
                          <Trash2 className="h-5 w-5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default AdminUserList;
