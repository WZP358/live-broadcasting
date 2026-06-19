import cv2
import numpy as np
import os
import uuid
import torch
from fastapi import FastAPI, UploadFile, File, HTTPException
from nudenet import NudeDetector

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
WEIGHTS_DIR = os.path.join(BASE_DIR, '..', 'weights')
YOLO_CONFIG_DIR = os.path.join(BASE_DIR, '.ultralytics')
os.makedirs(YOLO_CONFIG_DIR, exist_ok=True)
os.environ.setdefault("YOLO_CONFIG_DIR", YOLO_CONFIG_DIR)

from ultralytics import YOLO

app = FastAPI(title="Live Guard 2026 - Ultimate Edition")

# --- 1. 配置与模型初始化 ---
DEVICE = 'cuda' if torch.cuda.is_available() else 'cpu'

print(f"Using Device: {DEVICE}")
obj_model = YOLO(os.path.join(WEIGHTS_DIR, 'yolov8l.pt')).to(DEVICE)
pose_model = YOLO(os.path.join(WEIGHTS_DIR, 'yolov8l-pose.pt')).to(DEVICE)
nude_detector = NudeDetector()

# 严控关键词
RISK_KEYWORDS = {'knife', 'scissors', 'dagger', 'sword', 'blade', 'pistol', 'rifle', 'weapon', 'gun', 'axe', 'machete'}


def check_violence_pose(keypoints_obj):
    """
    分析关键点：检测脚部是否接近他人头部 (霸凌场景)
    """
    if keypoints_obj is None or keypoints_obj.data.shape[0] < 2:
        return False

    kpts = keypoints_obj.data.cpu().numpy()  # 转移到 CPU 进行逻辑计算
    for i, p1 in enumerate(kpts):
        if p1[0][2] < 0.55:
            continue
        head_p1 = p1[0][:2]  # 鼻尖坐标
        for j, p2 in enumerate(kpts):
            if i == j: continue
            left_foot_ok = p2[15][2] >= 0.55
            right_foot_ok = p2[16][2] >= 0.55
            if not left_foot_ok and not right_foot_ok:
                continue
            # 检查 p2 的左脚(15)或右脚(16)是否靠近 p1 的头(0)
            foot_p2_l = p2[15][:2]
            foot_p2_r = p2[16][:2]

            dist_l = np.linalg.norm(head_p1 - foot_p2_l) if left_foot_ok else float("inf")
            dist_r = np.linalg.norm(head_p1 - foot_p2_r) if right_foot_ok else float("inf")

            # 距离阈值设定为 130 像素
            if dist_l < 90 or dist_r < 90:
                return True
    return False


@app.post("/check")
async def guard_endpoint(file: UploadFile = File(...)):
    # 读取图片流
    try:
        content = await file.read()
        nparr = np.frombuffer(content, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if frame is None: raise Exception
    except:
        raise HTTPException(status_code=400, detail="Image decode failed")

    # --- A. 武器检测 ---
    # imgsz=1280 配合 yolov8l 能极大提升对小刀具的捕获率
    obj_results = obj_model.predict(frame, conf=0.15, imgsz=1280, augment=True, verbose=False)[0]
    weapons = []
    for box in obj_results.boxes:
        label = obj_results.names[int(box.cls[0])].lower()
        conf = float(box.conf[0])
        if any(risk in label for risk in RISK_KEYWORDS):
            weapons.append({"label": label, "score": round(conf, 3)})

    # --- B. 肢体冲突 (Pose) ---
    pose_results = pose_model.predict(frame, verbose=False)[0]
    violence_act = check_violence_pose(pose_results.keypoints)

    # --- C. 色情检测 (NudeNet) ---
    temp_path = f"tmp_{uuid.uuid4()}.jpg"
    cv2.imwrite(temp_path, frame)
    try:
        nude_data = nude_detector.detect(temp_path)
        bad_tags = {'female_breast_exposed', 'female_genitalia_exposed', 'buttocks_exposed'}
        nude_violation = any(n['class'] in bad_tags and n['score'] > 0.6 for n in nude_data)
    finally:
        if os.path.exists(temp_path): os.remove(temp_path)

    # --- D. 综合评估逻辑 ---
    # 规则：1. 有裸露或暴力动作 -> REJECT
    #      2. 有武器且置信度高 (>0.4) -> REJECT
    #      3. 有武器但置信度低 (0.15~0.4) -> REVIEW
    weapon_violation = any(w['score'] > 0.4 for w in weapons)
    violation_types = []
    if weapon_violation or weapons:
        violation_types.append({"type": "WEAPON", "label": "违规刀具"})
    if violence_act:
        violation_types.append({"type": "VIOLENCE", "label": "暴力行为"})
    if nude_violation:
        violation_types.append({"type": "EXPOSURE", "label": "过于暴露"})

    status = "SAFE"
    if nude_violation or violence_act or weapon_violation:
        status = "REJECT"
    elif weapons:
        status = "REVIEW"
    primary_violation = violation_types[0] if violation_types else {"type": "NONE", "label": ""}

    return {
        "status": status,
        "is_safe": status == "SAFE",
        "violation_type": primary_violation["type"],
        "violation_label": primary_violation["label"],
        "violation_types": violation_types,
        "evidence": {
            "weapons": weapons,
            "physical_violence": violence_act,
            "nude_detected": nude_violation
        }
    }


if __name__ == "__main__":
    import uvicorn

    port = int(os.environ.get("PULSELIVE_GUARD_PORT", "8300"))
    uvicorn.run(app, host="0.0.0.0", port=port)
