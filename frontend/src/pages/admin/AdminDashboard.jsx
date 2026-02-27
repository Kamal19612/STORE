import { useState, useEffect } from "react";
import api from "../../services/api";
import { motion } from "framer-motion";
import {
  ShoppingBag,
  Package,
  TrendingUp,
  Clock,
  CheckCircle,
  BarChart3,
  Calendar,
  ArrowUpRight,
  ArrowDownRight,
  Activity,
  Users,
} from "lucide-react";

/**
 * Composant Sparkline SVG simple pour les tendances
 */
const Sparkline = ({ data, color }) => (
  <svg viewBox="0 0 100 40" className="w-full h-12">
    <motion.path
      initial={{ pathLength: 0, opacity: 0 }}
      animate={{ pathLength: 1, opacity: 1 }}
      transition={{ duration: 1.5, ease: "easeInOut" }}
      d={`M 0 35 ${data.map((val, i) => `L ${i * (100 / (data.length - 1))} ${35 - val}`).join(" ")}`}
      fill="none"
      stroke={color}
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

/**
 * Dashboard Admin - Design Analytique & Data-focused
 */
const AdminDashboard = () => {
  const [stats, setStats] = useState({
    totalOrders: 0,
    totalProducts: 0,
    totalRevenue: 0,
    pendingOrders: 0,
    confirmedOrders: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await api.get("/admin/dashboard/stats");
        setStats(response.data);
      } catch (err) {
        console.error("Erreur lors du chargement des statistiques:", err);
        setError("Impossible de charger les statistiques");
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
          className="rounded-full h-10 w-10 border-4 border-gray-200 border-t-primary"
        />
      </div>
    );
  }

  const cards = [
    {
      label: "Chiffre d'Affaires",
      value: `${stats.totalRevenue.toLocaleString()} FCFA`,
      trend: "+12.5%",
      isUp: true,
      data: [10, 15, 8, 20, 18, 25, 22],
      color: "#10b981", // emerald-500
      icon: TrendingUp,
    },
    {
      label: "Commandes",
      value: stats.totalOrders,
      trend: "+5.2%",
      isUp: true,
      data: [5, 12, 10, 15, 12, 18, 20],
      color: "#3b82f6", // blue-500
      icon: ShoppingBag,
    },
    {
      label: "Produits",
      value: stats.totalProducts,
      trend: "-2.1%",
      isUp: false,
      data: [20, 18, 19, 17, 18, 16, 17],
      color: "#8b5cf6", // purple-500
      icon: Package,
    },
  ];

  return (
    <div className="space-y-6 p-4">
      {/* Header Analytique */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b dark:border-white/10 pb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Analyse de Performance
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Suivi en temps réel de votre activité commerciale
          </p>
        </div>
        <div className="flex items-center gap-3 bg-white dark:bg-secondary p-2 rounded-lg border border-gray-100 dark:border-white/5 shadow-sm text-sm font-medium">
          <Calendar className="w-4 h-4 text-primary" />
          <span className="dark:text-gray-300">Derniers 7 jours</span>
        </div>
      </div>

      {/* Grid de Cartes Analytiques */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {cards.map((card, idx) => (
          <motion.div
            key={idx}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.1 }}
            className="bg-white dark:bg-secondary rounded-xl p-6 shadow-sm border border-gray-100 dark:border-white/5 hover:border-primary/30 transition-colors"
          >
            <div className="flex items-start justify-between mb-4">
              <div className="p-2 bg-gray-50 dark:bg-white/5 rounded-lg">
                <card.icon className="w-5 h-5 text-gray-500 dark:text-gray-400" />
              </div>
              <div
                className={`flex items-center gap-0.5 text-xs font-bold ${card.isUp ? "text-emerald-500" : "text-red-500"}`}
              >
                {card.isUp ? (
                  <ArrowUpRight className="w-3 h-3" />
                ) : (
                  <ArrowDownRight className="w-3 h-3" />
                )}
                {card.trend}
              </div>
            </div>

            <div className="space-y-1 mb-4">
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                {card.label}
              </p>
              <h3 className="text-2xl font-bold text-gray-900 dark:text-white tracking-tight">
                {card.value}
              </h3>
            </div>

            <Sparkline data={card.data} color={card.color} />
          </motion.div>
        ))}
      </div>

      {/* Détails et Activité */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Progress Section */}
        <motion.div
          initial={{ opacity: 0, x: -10 }}
          animate={{ opacity: 1, x: 0 }}
          className="lg:col-span-2 bg-white dark:bg-secondary rounded-xl p-6 shadow-sm border border-gray-100 dark:border-white/5"
        >
          <h4 className="flex items-center gap-2 text-lg font-bold text-gray-900 dark:text-white mb-6">
            <Activity className="w-5 h-5 text-primary" />
            État des Opérations
          </h4>

          <div className="grid md:grid-cols-2 gap-8">
            <div className="space-y-6">
              <div>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-gray-500 dark:text-gray-400">
                    Commandes en attente
                  </span>
                  <span className="font-bold dark:text-white">
                    {stats.pendingOrders}
                  </span>
                </div>
                <div className="h-2 bg-gray-100 dark:bg-white/10 rounded-full overflow-hidden">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{
                      width: `${(stats.pendingOrders / (stats.totalOrders || 1)) * 100}%`,
                    }}
                    className="h-full bg-amber-400"
                  />
                </div>
              </div>
              <div>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-gray-500 dark:text-gray-400">
                    Commandes confirmées
                  </span>
                  <span className="font-bold dark:text-white">
                    {stats.confirmedOrders}
                  </span>
                </div>
                <div className="h-2 bg-gray-100 dark:bg-white/10 rounded-full overflow-hidden">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{
                      width: `${(stats.confirmedOrders / (stats.totalOrders || 1)) * 100}%`,
                    }}
                    className="h-full bg-emerald-500"
                  />
                </div>
              </div>
            </div>

            <div className="bg-primary/5 dark:bg-primary/10 rounded-xl p-6 flex flex-col items-center justify-center text-center">
              <Users className="w-10 h-10 text-primary mb-3" />
              <p className="text-2xl font-bold text-primary">
                {Math.round(
                  (stats.confirmedOrders / (stats.totalOrders || 1)) * 100,
                )}
                %
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Taux de conversion commandes
              </p>
            </div>
          </div>
        </motion.div>

        {/* Quick Summary */}
        <motion.div
          initial={{ opacity: 0, x: 10 }}
          animate={{ opacity: 1, x: 0 }}
          className="bg-white dark:bg-secondary rounded-xl p-6 shadow-sm border border-gray-100 dark:border-white/5"
        >
          <h4 className="text-lg font-bold text-gray-900 dark:text-white mb-6">
            Récapitulatif Rapide
          </h4>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 rounded-lg bg-gray-50 dark:bg-white/5">
              <div className="flex items-center gap-3">
                <div className="w-2 h-2 rounded-full bg-blue-500" />
                <span className="text-sm font-medium dark:text-gray-300">
                  Total Ventes
                </span>
              </div>
              <span className="text-sm font-bold dark:text-white">
                {stats.totalOrders}
              </span>
            </div>
            <div className="flex items-center justify-between p-3 rounded-lg bg-gray-50 dark:bg-white/5">
              <div className="flex items-center gap-3">
                <div className="w-2 h-2 rounded-full bg-green-500" />
                <span className="text-sm font-medium dark:text-gray-300">
                  Stock Produits
                </span>
              </div>
              <span className="text-sm font-bold dark:text-white">
                {stats.totalProducts}
              </span>
            </div>
            <div className="mt-6">
              <button className="w-full bg-secondary dark:bg-primary dark:text-secondary text-white font-bold py-3 rounded-xl hover:opacity-90 transition-opacity flex items-center justify-center gap-2">
                <BarChart3 className="w-4 h-4" />
                Rapport Détaillé
              </button>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
};

export default AdminDashboard;
