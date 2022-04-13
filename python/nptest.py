
import numpy as np


def calculateA(tp):
    m = np.array([[0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 1.0]], np.float64)
    pi = np.pi
    t1 = tp[0]*pi/180.0
    t2 = tp[1]*pi/180.0
    t3 = tp[2]*pi/180.0
    gamma = np.sqrt(t1*t1+t2*t2)
    phi = np.arctan2(t1,t2)
    omega = t3
    sp = np.sin(omega/2.0+phi)
    cp = np.cos(omega/2.0+phi)
    sm = np.sin(omega/2.0-phi)
    cm = np.cos(omega/2.0-phi)
    sg = np.sin(gamma)
    cg = np.cos(gamma)
    m[0][0] = cm*cg*cp-sm*sp
    m[0][1] = -cm*cg*sp-sm*cp
    m[0][2] = cm*sg
    m[1][0] = sm*cg*cp+cm*sp
    m[1][1] = -sm*cg*sp+cm*cp
    m[1][2] = sm*sg
    m[2][0] = -sg*cp
    m[2][1] = sg*sp
    m[2][2] = cg

    sp = np.sin(phi); cp = np.cos(phi); sg = np.sin(gamma/2.0); cg = np.cos(gamma/2.0)
    m[0][3] = tp[3]*(cm*cg*cp-sm*sp) + tp[4]*(-cm*cg*sp-sm*cp) + tp[5]*(cm*sg)
    m[1][3] = tp[3]*(sm*cg*cp+cm*sp) + tp[4]*(-sm*cg*sp-cm*cp) + tp[5]*(sm*sg)
    m[2][3] = tp[3]*(-sg*cp) + tp[4]*(sg*sp) + tp[5]*cg

    return m


print(calculateA([0, 0, 36, 0, 0, 3.4]))
