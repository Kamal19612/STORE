import { useEffect, useState } from "react";
import { getPublicSettings } from "../../services/api";

const Footer = () => {
  const [settings, setSettings] = useState({});

  useEffect(() => {
    const fetchSettings = async () => {
      try {
        const response = await getPublicSettings();
        setSettings(response.data);
      } catch (error) {
        console.error("Erreur chargement footer:", error);
      }
    };
    fetchSettings();
  }, []);

  return (
    <footer className="bg-secondary text-gray-400 py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="col-span-1 md:col-span-2">
            <img
              src="/logo-sucre.png"
              alt="Sucre Store"
              className="h-12 w-auto mb-4"
            />
            <p className="mt-4 max-w-xs">
              Votre destination privilégiée pour toutes vos envies sucrées. Des
              produits de qualité livrés directement chez vous.
            </p>
            <div className="mt-4 flex space-x-4">
              {settings.social_facebook && (
                <a
                  href={settings.social_facebook}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-gray-400 hover:text-white transition-colors"
                >
                  <i className="fab fa-facebook text-xl"></i>
                </a>
              )}
              {settings.social_instagram && (
                <a
                  href={settings.social_instagram}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-gray-400 hover:text-white transition-colors"
                >
                  <i className="fab fa-instagram text-xl"></i>
                </a>
              )}
            </div>
          </div>

          <div>
            <h3 className="text-white font-bold mb-4 uppercase tracking-widest text-sm">
              Liens Utiles
            </h3>
            <ul className="space-y-2 text-sm">
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Mentions Légales
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Conditions de Vente
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-primary transition-colors">
                  Politique de Confidentialité
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-bold mb-4 uppercase tracking-widest text-sm">
              Contact
            </h3>
            <ul className="space-y-2 text-sm">
              <li>📍 {settings.contact_address || "Abidjan, Côte d'Ivoire"}</li>
              <li>📞 {settings.contact_phone || "+225 07 07 07 07 07"}</li>
              <li>✉️ {settings.contact_email || "contact@sucrestore.com"}</li>
            </ul>
          </div>
        </div>

        <div className="mt-12 pt-8 border-t border-secondary-light text-center text-xs">
          <p>
            &copy; {new Date().getFullYear()} Sucre Store.{" "}
            {settings.footer_copyright || "Tous droits réservés."} Design & Code
            avec ❤️
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
