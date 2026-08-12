import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axios";
import { useAuth } from "../context/AuthContext";

export default function AdminPanel() {
  const { user, logout, updateRoles } = useAuth();
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [confirmTarget, setConfirmTarget] = useState(null); // user pending delete confirmation
  const [confirmSelfDemote, setConfirmSelfDemote] = useState(false);

  const adminCount = users.filter((u) => u.roles?.includes("ROLE_ADMIN")).length;
  const isLastAdmin = adminCount <= 1;

  const loadUsers = async () => {
    try {
      const res = await api.get("/admin/users");
      setUsers(res.data);
    } catch {
      setError("Could not load users. You may not have admin access.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const toggleActive = async (u) => {
    const action = u.active ? "deactivate" : "activate";
    try {
      await api.put(`/admin/users/${u.id}/${action}`);
      loadUsers();
    } catch {
      setError(`Could not ${action} ${u.username}.`);
    }
  };

  const makeAdmin = async (u) => {
    setError("");
    try {
      await api.put(`/admin/users/${u.id}/make-admin`);
      loadUsers();
    } catch (err) {
      setError(err.response?.data?.error || `Could not make ${u.username} an admin.`);
    }
  };

  const deleteUser = async (u) => {
    setError("");
    try {
      await api.delete(`/admin/users/${u.id}`);
      setConfirmTarget(null);
      loadUsers();
    } catch (err) {
      setError(err.response?.data?.error || `Could not delete ${u.username}.`);
    }
  };

  const removeSelfAdmin = async () => {
    setError("");
    try {
      const res = await api.put("/admin/self/remove-admin");
      updateRoles(res.data.roles);
      setConfirmSelfDemote(false);
      navigate("/dashboard");
    } catch (err) {
      setError(err.response?.data?.error || "Could not remove your admin access.");
      setConfirmSelfDemote(false);
    }
  };

  const viewAudit = async (username) => {
    setSelectedUser(username);
    try {
      const res = await api.get(`/admin/audit/${username}`);
      setAuditLogs(res.data);
    } catch {
      setAuditLogs([]);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center">
              <span className="text-indigo-400 text-sm font-semibold">ID</span>
            </div>
            <span className="font-semibold">IdentitySoft</span>
            <span className="text-xs text-slate-500 ml-1 px-2 py-0.5 rounded-full border border-slate-700">Admin</span>
          </div>
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate("/dashboard")}
              className="text-sm text-slate-400 hover:text-slate-200 transition"
            >
              Dashboard
            </button>
            <button
              onClick={() => setConfirmSelfDemote(true)}
              disabled={isLastAdmin}
              title={isLastAdmin ? "You are the only admin — promote someone else first" : undefined}
              className="text-sm rounded-lg border border-slate-700 hover:border-slate-600 hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:border-slate-700 disabled:hover:bg-transparent px-3 py-1.5 transition focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              Remove my admin access
            </button>
            <button
              onClick={() => { logout(); navigate("/login"); }}
              className="text-sm rounded-lg border border-slate-700 hover:border-slate-600 hover:bg-slate-800 px-3 py-1.5 transition focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-semibold">User management</h1>
        <p className="text-slate-400 mt-1">Activate or deactivate accounts and review activity.</p>

        {error && (
          <div role="alert" className="mt-6 rounded-lg bg-red-500/10 border border-red-500/30 px-4 py-3 text-sm text-red-300">
            {error}
          </div>
        )}

        {loading ? (
          <p className="text-slate-400 text-sm mt-8">Loading users…</p>
        ) : (
          <div className="mt-8 overflow-hidden rounded-2xl border border-slate-800">
            <table className="w-full text-sm">
              <thead className="bg-slate-900 text-slate-400">
                <tr>
                  <th className="text-left font-medium px-5 py-3">User</th>
                  <th className="text-left font-medium px-5 py-3">Email</th>
                  <th className="text-left font-medium px-5 py-3">Role</th>
                  <th className="text-left font-medium px-5 py-3">Status</th>
                  <th className="text-right font-medium px-5 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {users.map((u) => {
                  const userIsAdmin = u.roles?.includes("ROLE_ADMIN");
                  const isSelf = u.username === user?.username;
                  return (
                    <tr key={u.id} className="bg-slate-900/40 hover:bg-slate-900 transition">
                      <td className="px-5 py-3.5 font-medium">
                        {u.username}
                        {isSelf && <span className="ml-2 text-xs text-slate-500">(you)</span>}
                      </td>
                      <td className="px-5 py-3.5 text-slate-400">{u.email}</td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full ${
                          userIsAdmin
                            ? "bg-indigo-500/10 text-indigo-300 border border-indigo-500/20"
                            : "bg-slate-700/40 text-slate-400 border border-slate-600/40"
                        }`}>
                          {userIsAdmin ? "Admin" : "User"}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full ${
                          u.active
                            ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                            : "bg-slate-700/40 text-slate-400 border border-slate-600/40"
                        }`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${u.active ? "bg-emerald-400" : "bg-slate-500"}`} />
                          {u.active ? "Active" : "Inactive"}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-right space-x-2 whitespace-nowrap">
                        <button
                          onClick={() => viewAudit(u.username)}
                          className="text-xs rounded-md border border-slate-700 hover:bg-slate-800 px-2.5 py-1.5 transition"
                        >
                          Audit
                        </button>
                        <button
                          onClick={() => toggleActive(u)}
                          className={`text-xs rounded-md px-2.5 py-1.5 transition font-medium ${
                            u.active
                              ? "border border-red-500/30 text-red-300 hover:bg-red-500/10"
                              : "border border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/10"
                          }`}
                        >
                          {u.active ? "Deactivate" : "Activate"}
                        </button>
                        {!userIsAdmin && (
                          <button
                            onClick={() => makeAdmin(u)}
                            className="text-xs rounded-md border border-indigo-500/30 text-indigo-300 hover:bg-indigo-500/10 px-2.5 py-1.5 transition font-medium"
                          >
                            Make admin
                          </button>
                        )}
                        <button
                          onClick={() => setConfirmTarget(u)}
                          className="text-xs rounded-md border border-red-500/30 text-red-300 hover:bg-red-500/10 px-2.5 py-1.5 transition font-medium"
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Audit log panel */}
        {selectedUser && (
          <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-900/40 p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-semibold">Audit trail — {selectedUser}</h2>
              <button
                onClick={() => setSelectedUser(null)}
                className="text-xs text-slate-400 hover:text-slate-200"
              >
                Close
              </button>
            </div>
            {auditLogs.length === 0 ? (
              <p className="text-sm text-slate-400">No activity recorded yet.</p>
            ) : (
              <ul className="space-y-2">
                {auditLogs.map((log) => (
                  <li key={log.id} className="flex items-center justify-between text-sm border-b border-slate-800 pb-2 last:border-0">
                    <span className="font-mono text-slate-300">{log.action}</span>
                    <span className="text-slate-500 text-xs">
                      {new Date(log.timestamp).toLocaleString()}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </main>

      {/* Delete confirmation */}
      {confirmTarget && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center px-4 z-50">
          <div className="w-full max-w-sm bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
            <h2 className="font-semibold text-slate-100">Delete {confirmTarget.username}?</h2>
            <p className="text-sm text-slate-400 mt-2">
              This permanently removes the account and cannot be undone.
            </p>
            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setConfirmTarget(null)}
                className="text-sm rounded-lg border border-slate-700 hover:bg-slate-800 px-3.5 py-2 transition"
              >
                Cancel
              </button>
              <button
                onClick={() => deleteUser(confirmTarget)}
                className="text-sm rounded-lg bg-red-500 hover:bg-red-400 text-white font-medium px-3.5 py-2 transition"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Self-demote confirmation */}
      {confirmSelfDemote && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center px-4 z-50">
          <div className="w-full max-w-sm bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
            <h2 className="font-semibold text-slate-100">Remove your admin access?</h2>
            <p className="text-sm text-slate-400 mt-2">
              You'll immediately lose access to this panel. Another admin will need to
              re-promote you if you need it back.
            </p>
            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setConfirmSelfDemote(false)}
                className="text-sm rounded-lg border border-slate-700 hover:bg-slate-800 px-3.5 py-2 transition"
              >
                Cancel
              </button>
              <button
                onClick={removeSelfAdmin}
                className="text-sm rounded-lg bg-red-500 hover:bg-red-400 text-white font-medium px-3.5 py-2 transition"
              >
                Remove access
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}