import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axios";
import PasswordInput from "../components/PasswordInput";
import { isValidPassword, PASSWORD_HINT } from "../utils/validation";

export default function ForgotPassword() {
  const navigate = useNavigate();

  const [identifier, setIdentifier] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setError("");
    if (!identifier || !newPassword || !confirmPassword) {
      setError("Fill in every field to reset your password.");
      return;
    }
    if (!isValidPassword(newPassword)) {
      setError(`Password doesn't meet the requirements. ${PASSWORD_HINT}`);
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("New password and confirmation do not match.");
      return;
    }
    setLoading(true);
    try {
      await api.post("/auth/forgot-password", { identifier, newPassword, confirmPassword });
      setSuccess(true);
    } catch (err) {
      setError(err.response?.data?.error || "Could not reset your password. Try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 mb-4">
            <span className="text-indigo-400 text-xl font-semibold">ID</span>
          </div>
          <h1 className="text-2xl font-semibold text-slate-100">Reset your password</h1>
          <p className="text-sm text-slate-400 mt-1">Set a new password for your account</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-xl">
          <div className="mb-5 rounded-lg bg-slate-800/60 border border-slate-700 px-4 py-3 text-xs text-slate-400">
            Since this is a demo project no email verification is needed — just confirm your
            username or email and set a new password.
          </div>

          {error && (
            <div
              role="alert"
              className="mb-5 rounded-lg bg-red-500/10 border border-red-500/30 px-4 py-3 text-sm text-red-300"
            >
              {error}
            </div>
          )}

          {success ? (
            <div className="space-y-5">
              <div
                role="alert"
                className="rounded-lg bg-emerald-500/10 border border-emerald-500/30 px-4 py-3 text-sm text-emerald-300"
              >
                Your password has been reset. You can sign in with your new password now.
              </div>
              <button
                onClick={() => navigate("/login")}
                className="w-full rounded-lg bg-indigo-500 hover:bg-indigo-400 px-4 py-2.5 text-sm font-semibold text-white transition focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-slate-900"
              >
                Go to sign in
              </button>
            </div>
          ) : (
            <div className="space-y-5">
              <div>
                <label htmlFor="identifier" className="block text-sm font-medium text-slate-300 mb-1.5">
                  Username or email
                </label>
                <input
                  id="identifier"
                  type="text"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  className="w-full rounded-lg bg-slate-800 border border-slate-700 px-3.5 py-2.5 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
                  placeholder="Your username or email"
                  autoComplete="username"
                />
              </div>

              <div>
                <PasswordInput
                  id="newPassword"
                  label="New password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Create a new password"
                  autoComplete="new-password"
                />
                <p className="text-xs text-slate-500 mt-1.5">{PASSWORD_HINT}</p>
              </div>

              <PasswordInput
                id="confirmPassword"
                label="Confirm new password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
                placeholder="Re-enter your new password"
                autoComplete="new-password"
              />

              <button
                onClick={handleSubmit}
                disabled={loading}
                className="w-full rounded-lg bg-indigo-500 hover:bg-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed px-4 py-2.5 text-sm font-semibold text-white transition focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-slate-900"
              >
                {loading ? "Resetting…" : "Reset password"}
              </button>
            </div>
          )}
        </div>

        <p className="text-center text-sm text-slate-400 mt-6">
          Remembered your password?{" "}
          <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-medium">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
