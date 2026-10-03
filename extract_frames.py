import os
import cv2

video_files = [
    ("video1", "reference/1790562445530_334977966644786979_1343906172483461411.mp4"),
    ("video2", "reference/1790562445545_334977966644786979_1343906172483461411.mp4")
]

os.makedirs("docs/frames", exist_ok=True)

for name, path in video_files:
    if not os.path.exists(path):
        print(f"File not found: {path}")
        continue

    out_dir = os.path.join("docs/frames", name)
    os.makedirs(out_dir, exist_ok=True)

    cap = cv2.VideoCapture(path)
    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    duration = total_frames / fps if fps > 0 else 0

    print(f"\nProcessing {name}: {path}")
    print(f"FPS: {fps:.2f}, Total frames: {total_frames}, Duration: {duration:.2f}s")

    # Extract 2 frames per second (interval = fps / 2)
    step = max(1, int(fps / 2))
    frame_idx = 0
    saved_count = 0

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        if frame_idx % step == 0:
            timestamp_sec = frame_idx / fps
            out_filename = f"{name}_f{saved_count:04d}_t{timestamp_sec:05.1f}s.jpg"
            out_path = os.path.join(out_dir, out_filename)
            cv2.imwrite(out_path, frame)
            saved_count += 1

        frame_idx += 1

    cap.release()
    print(f"Extracted {saved_count} frames to {out_dir}")
