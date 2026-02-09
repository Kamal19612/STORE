const Footer = () => {
  return (
    <footer className="bg-secondary text-gray-400 py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="col-span-1 md:col-span-2">
            <span className="text-2xl font-black tracking-tighter text-primary italic">
              SUCRE<span className="text-white not-italic">STORE</span>
            </span>
            <p className="mt-4 max-w-xs">
              Votre destination privilégiée pour toutes vos envies sucrées. Des
              produits de qualité livrés directement chez vous.
            </p>
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
              <li>📍 Abidjan, Côte d'Ivoire</li>
              <li>📞 +225 07 07 07 07 07</li>
              <li>✉️ contact@sucrestore.com</li>
            </ul>
          </div>
        </div>

        <div className="mt-12 pt-8 border-t border-secondary-light text-center text-xs">
          <p>
            &copy; {new Date().getFullYear()} Sucre Store. Tous droits réservés.
            Design & Code avec ❤️
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
