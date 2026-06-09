import os
from pathlib import Path

from dotenv import load_dotenv
from flask import Flask

from models import db
from routes_api import bp as api_bp


def create_app() -> Flask:
    load_dotenv()
    app = Flask(__name__, instance_relative_config=True)

    instance_dir = Path(app.instance_path)
    instance_dir.mkdir(parents=True, exist_ok=True)
    upload_dir = Path(os.getenv("GEPSI_UPLOAD_DIR", instance_dir.parent / "uploads"))
    upload_dir.mkdir(parents=True, exist_ok=True)

    db_path = instance_dir / "gepsi.db"
    app.config.update(
        SQLALCHEMY_DATABASE_URI=f"sqlite:///{db_path}",
        SQLALCHEMY_TRACK_MODIFICATIONS=False,
        MAX_CONTENT_LENGTH=25 * 1024 * 1024,
        UPLOAD_DIR=str(upload_dir),
    )

    db.init_app(app)
    app.register_blueprint(api_bp)

    with app.app_context():
        db.create_all()

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
