import { HashRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider, Protected } from './auth';
import Layout from './components/Layout';
import AdminHome from './pages/AdminHome';
import DoctorDetail from './pages/DoctorDetail';
import DoctorHome from './pages/DoctorHome';
import Home from './pages/Home';
import Login from './pages/Login';
import NotFound from './pages/NotFound';
import PatientHome from './pages/PatientHome';
import Register from './pages/Register';

export default function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route element={<Layout />}>
            <Route path="/" element={<Home />} />
            <Route
              path="/patient"
              element={
                <Protected role="PATIENT">
                  <PatientHome />
                </Protected>
              }
            />
            <Route
              path="/doctor"
              element={
                <Protected role="DOCTOR">
                  <DoctorHome />
                </Protected>
              }
            />
            <Route
              path="/admin"
              element={
                <Protected role="ADMIN">
                  <AdminHome />
                </Protected>
              }
            />
            <Route
              path="/doctors/:id"
              element={
                <Protected role="PATIENT">
                  <DoctorDetail />
                </Protected>
              }
            />
            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </HashRouter>
    </AuthProvider>
  );
}
