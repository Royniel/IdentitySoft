import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Top bar */}
      <header className="border-b border-slate-800 bg-slate-900/50">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center">
              <span className="text-indigo-400 text-sm font-semibold">ID</span>
            </div>
            <span className="font-semibold">IdentitySoft</span>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-400">{user?.username}</span>
            <button
              onClick={handleLogout}
              className="text-sm rounded-lg border border-slate-700 hover:border-slate-600 hover:bg-slate-800 px-3 py-1.5 transition focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              Sign out
            </button>
          </div>
        </div>
      </header>

      {/* Body */}
      <main className="max-w-5xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-semibold">Welcome back, {user?.username}</h1>
        <p className="text-slate-400 mt-1">Here's an overview of your identity console.</p>

        {/* Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mt-8">
          <div className="rounded-2xl bg-slate-900 border border-slate-800 p-6">
            <p className="text-sm text-slate-400">Account status</p>
            <p className="text-lg font-semibold mt-1 text-emerald-400">Active</p>
          </div>
          <div className="rounded-2xl bg-slate-900 border border-slate-800 p-6">
            <p className="text-sm text-slate-400">Signed in as</p>
            <p className="text-lg font-semibold mt-1">{user?.username}</p>
          </div>
          <div className="rounded-2xl bg-slate-900 border border-slate-800 p-6">
            <p className="text-sm text-slate-400">Session</p>
            <p className="text-lg font-semibold mt-1">JWT secured</p>
          </div>
        </div>

        {/* Admin entry — visible to everyone, but only clickable for admins; the full user list lives behind this, never on the home page */}
        <div className="mt-8 rounded-2xl bg-slate-900 border border-slate-800 p-6 flex items-center justify-between">
          <div>
            <p className="font-medium">Administration</p>
            <p className="text-sm text-slate-400 mt-0.5">
              View all users, manage roles, and review the audit trail.
            </p>
            {!isAdmin && (
              <p className="text-xs text-amber-400/80 mt-1.5">Log in as an admin to use the admin panel.</p>
            )}
          </div>
          <button
            onClick={() => navigate("/admin")}
            disabled={!isAdmin}
            title={!isAdmin ? "Log in as an admin to use the admin panel" : undefined}
            className="rounded-lg bg-indigo-500 hover:bg-indigo-400 disabled:bg-slate-800 disabled:text-slate-500 disabled:cursor-not-allowed disabled:hover:bg-slate-800 px-4 py-2 text-sm font-semibold text-white transition focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-slate-900"
          >
            View all users
          </button>
        </div>
      </main>
    </div>
  );
}
