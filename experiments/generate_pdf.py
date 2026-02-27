"""
Generate PDF presentation for AI-Platform for Automating Bioequivalence Research (Oncology).
Uses reportlab to create a PDF with the same content as the PPTX.
Based on issue #5: https://github.com/medmancifra/sendbox/issues/5
"""

import os
from reportlab.lib.pagesizes import landscape, A4
from reportlab.lib.units import inch
from reportlab.lib.colors import Color, HexColor, white, black
from reportlab.platypus import SimpleDocTemplate, Spacer
from reportlab.pdfgen import canvas
from reportlab.lib.utils import simpleSplit

# Page size: landscape A4
PAGE_W, PAGE_H = landscape(A4)

# Color palette
C_BLUE_DARK = HexColor("#1A237E")
C_BLUE_MED = HexColor("#1565C0")
C_BLUE_LIGHT = HexColor("#42A5F5")
C_PURPLE = HexColor("#6A1B9A")
C_GREEN = HexColor("#2E7D32")
C_TEAL = HexColor("#00897B")
C_WHITE = white
C_BLACK = HexColor("#212121")
C_GRAY_LIGHT = HexColor("#F5F5F5")
C_GRAY_MED = HexColor("#9E9E9E")
C_ACCENT_ORANGE = HexColor("#EF6C00")
C_ACCENT_RED = HexColor("#C62828")


def hex_color(hex_str):
    return HexColor(hex_str)


class Slide:
    """A helper to draw slide elements on a reportlab canvas page."""

    def __init__(self, c: canvas.Canvas, w=PAGE_W, h=PAGE_H):
        self.c = c
        self.w = w
        self.h = h

    def background(self, color):
        self.c.setFillColor(color)
        self.c.rect(0, 0, self.w, self.h, fill=1, stroke=0)

    def rect(self, x, y, w, h, fill_color, stroke_color=None, radius=3):
        """Draw rectangle (y from top)."""
        bottom = self.h - y - h
        self.c.setFillColor(fill_color)
        if stroke_color:
            self.c.setStrokeColor(stroke_color)
            self.c.roundRect(x, bottom, w, h, radius, fill=1, stroke=1)
        else:
            self.c.setStrokeColor(fill_color)
            self.c.roundRect(x, bottom, w, h, radius, fill=1, stroke=0)

    def text(self, text, x, y, font="Helvetica", size=12, color=C_BLACK,
             bold=False, italic=False, align="left", max_width=None):
        """Draw text (y from top)."""
        bottom = self.h - y
        if bold and italic:
            font = "Helvetica-BoldOblique"
        elif bold:
            font = "Helvetica-Bold"
        elif italic:
            font = "Helvetica-Oblique"
        self.c.setFont(font, size)
        self.c.setFillColor(color)

        if max_width and len(text) > 0:
            lines = simpleSplit(text, font, size, max_width)
            line_h = size * 1.3
            for i, line in enumerate(lines):
                if align == "center":
                    self.c.drawCentredString(x, bottom - i * line_h, line)
                elif align == "right":
                    self.c.drawRightString(x, bottom - i * line_h, line)
                else:
                    self.c.drawString(x, bottom - i * line_h, line)
        else:
            if align == "center":
                self.c.drawCentredString(x, bottom, text)
            elif align == "right":
                self.c.drawRightString(x, bottom, text)
            else:
                self.c.drawString(x, bottom, text)

    def multiline(self, lines_list, x, y, font="Helvetica", size=11,
                  color=C_BLACK, bold=False, line_spacing=1.4, max_width=None):
        """Draw multiple lines of text."""
        font_name = "Helvetica-Bold" if bold else "Helvetica"
        self.c.setFont(font_name, size)
        self.c.setFillColor(color)
        line_h = size * line_spacing
        for i, line in enumerate(lines_list):
            bottom = self.h - y - i * line_h
            if max_width:
                wrapped = simpleSplit(line, font_name, size, max_width)
                for j, wline in enumerate(wrapped):
                    self.c.drawString(x, bottom - j * line_h, wline)
            else:
                self.c.drawString(x, bottom, line)

    def rect_text(self, x, y, w, h, fill_color, text, text_color=C_WHITE,
                  font_size=12, bold=False, align="center", padding=5):
        """Draw a filled rectangle with centered text inside."""
        self.rect(x, y, w, h, fill_color)
        cx = x + w / 2
        cy = y + h / 2 - font_size * 0.35
        font_name = "Helvetica-Bold" if bold else "Helvetica"

        # Handle multi-line text
        lines = text.split("\n")
        total_h = len(lines) * font_size * 1.3
        start_y = y + (h - total_h) / 2

        self.c.setFont(font_name, font_size)
        self.c.setFillColor(text_color)
        for i, line in enumerate(lines):
            line_y = start_y + i * font_size * 1.3 + font_size * 0.3
            bottom = self.h - line_y - font_size * 0.5
            # Trim long lines
            if align == "center":
                wrapped = simpleSplit(line, font_name, font_size, w - 2 * padding)
                for j, wl in enumerate(wrapped):
                    self.c.drawCentredString(cx, bottom - j * font_size * 1.3, wl)
                    break  # only first line to avoid overflow
            elif align == "left":
                wrapped = simpleSplit(line, font_name, font_size, w - 2 * padding)
                for j, wl in enumerate(wrapped):
                    self.c.drawString(x + padding, bottom - j * font_size * 1.3, wl)
                    break

    def slide_number(self, num, total, color=C_WHITE):
        self.text(f"{num} / {total}", self.w - 0.6 * inch, self.h - 0.25 * inch,
                  size=9, color=color, align="right")

    def header_bar(self, title, fill_color, font_size=22, text_color=C_WHITE, height=0.7 * inch):
        self.rect(0, 0, self.w, height, fill_color)
        mid_x = self.w / 2
        self.c.setFont("Helvetica-Bold", font_size)
        self.c.setFillColor(text_color)
        bottom = self.h - height / 2 - font_size * 0.35
        self.c.drawCentredString(mid_x, bottom, title)


def draw_title_slide(c):
    s = Slide(c)
    s.background(C_BLUE_DARK)

    # Top accent bar
    s.rect(0, 0, PAGE_W, 0.06 * inch, C_BLUE_LIGHT)

    # Main title
    s.c.setFont("Helvetica-Bold", 34)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(PAGE_W / 2, PAGE_H - 1.6 * inch, "AI-Platform for Automating")
    s.c.drawCentredString(PAGE_W / 2, PAGE_H - 2.1 * inch, "Bioequivalence Research")

    # Oncology tag
    s.rect(PAGE_W / 2 - 1.4 * inch, 2.1 * inch, 2.8 * inch, 0.45 * inch, C_PURPLE)
    s.c.setFont("Helvetica-Bold", 15)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(PAGE_W / 2, 2.2 * inch, "ONCOLOGY")

    # Subtitle
    s.c.setFont("Helvetica", 13)
    s.c.setFillColor(C_BLUE_LIGHT)
    s.c.drawCentredString(PAGE_W / 2, 1.7 * inch, "Digital Health  |  AI-Architecture  |  Hackathon / Invest-Pitch")

    # Slogan
    s.c.setFont("Helvetica-Oblique", 12)
    s.c.setFillColor(HexColor("#BDBDBD"))
    s.c.drawCentredString(PAGE_W / 2, 1.2 * inch, '"We make doctors\' lives more cloud-based and IT-friendly"')

    # Bottom bar
    s.rect(0, PAGE_H - 0.5 * inch, PAGE_W, 0.5 * inch, C_PURPLE)
    s.c.setFont("Helvetica", 10)
    s.c.setFillColor(HexColor("#BDBDBD"))
    s.c.drawCentredString(PAGE_W / 2, PAGE_H - 0.32 * inch,
                          "konard / medmancifra  ·  medmancifra/sendbox  ·  2024")

    s.slide_number(1, 9)


def draw_problem_slide(c):
    s = Slide(c)
    s.background(C_WHITE)
    s.header_bar("Bioequivalence Problem in Oncology", C_BLUE_DARK)

    problems = [
        ("CV >30%", "High intra-subject variability requires RSABE instead of standard ABE"),
        ("Long t1/2", "t1/2 > 24-72h: complex washout planning, accumulation risk"),
        ("Multi-regulatory", "EMA/FDA/EEC No.85 require different criteria — manual reconciliation"),
        ("Fasting/Fed", "Many onco drugs need two separate studies for food effects"),
        ("Human factor", "Manual calculations lead to errors, inconsistencies, audit failures"),
    ]

    y_start = 0.9 * inch
    for i, (title, desc) in enumerate(problems):
        y = y_start + i * 0.72 * inch
        # Icon box
        s.rect(0.25 * inch, y, 0.5 * inch, 0.5 * inch, C_BLUE_LIGHT)
        s.c.setFont("Helvetica-Bold", 9)
        s.c.setFillColor(C_WHITE)
        s.c.drawCentredString(0.5 * inch, PAGE_H - y - 0.32 * inch, str(i + 1))
        # Title
        s.c.setFont("Helvetica-Bold", 13)
        s.c.setFillColor(C_BLUE_DARK)
        s.c.drawString(0.9 * inch, PAGE_H - y - 0.22 * inch, title)
        # Description
        s.c.setFont("Helvetica", 11)
        s.c.setFillColor(C_BLACK)
        s.c.drawString(0.9 * inch, PAGE_H - y - 0.45 * inch, desc)

    # Bottom result box
    s.rect(0.25 * inch, PAGE_H - 0.9 * inch, PAGE_W - 0.5 * inch, 0.55 * inch, C_ACCENT_RED)
    s.c.setFont("Helvetica-Bold", 12)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(PAGE_W / 2, PAGE_H - 0.65 * inch,
                          "Result: 2-5 days of manual planning per study => errors, regulatory risks, delays")

    s.slide_number(2, 9, C_BLUE_DARK)


def draw_solution_slide(c):
    s = Slide(c)
    s.background(C_GRAY_LIGHT)
    s.header_bar("Regulatory-Aware AI Planning Engine", C_BLUE_MED)

    blocks = [
        (C_BLUE_DARK, "Deterministic Core", "No LLM in math\nOwen's method\nISO/GOST compliant"),
        (C_PURPLE, "AI Explanation Layer", "GigaChat RAG\nNatural language\nexplanations"),
        (C_TEAL, "Onco-Specific Logic", "RSABE module\nCYP interactions\nHigh-variability protocols"),
        (C_GREEN, "Synopsis Generation", "Auto-generated\nMarkdown/JSON\nRegulatory-ready"),
    ]

    block_w = (PAGE_W - 1.0 * inch) / 4 - 0.1 * inch
    for i, (color, title, desc) in enumerate(blocks):
        x = 0.4 * inch + i * (block_w + 0.12 * inch)
        # Header
        s.rect(x, 0.85 * inch, block_w, 0.55 * inch, color)
        s.c.setFont("Helvetica-Bold", 11)
        s.c.setFillColor(C_WHITE)
        s.c.drawCentredString(x + block_w / 2, PAGE_H - 0.85 * inch - 0.35 * inch, title)
        # Content
        s.rect(x, 1.45 * inch, block_w, 1.7 * inch, C_WHITE)
        s.c.setFont("Helvetica", 10)
        s.c.setFillColor(C_BLACK)
        for j, line in enumerate(desc.split("\n")):
            s.c.drawString(x + 0.1 * inch, PAGE_H - 1.45 * inch - 0.25 * inch - j * 0.35 * inch, line)

    # Flow arrow
    y_flow = PAGE_H - 3.4 * inch
    s.rect(0.25 * inch, 3.2 * inch, 1.5 * inch, 0.9 * inch, C_BLUE_LIGHT)
    s.c.setFont("Helvetica-Bold", 10)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(1.0 * inch, y_flow + 0.05 * inch, "INPUT")
    s.c.setFont("Helvetica", 9)
    s.c.drawCentredString(1.0 * inch, y_flow - 0.2 * inch, "CV, t1/2, food")

    s.c.setFont("Helvetica-Bold", 16)
    s.c.setFillColor(C_BLUE_MED)
    s.c.drawString(1.95 * inch, y_flow, "=>")

    s.rect(2.6 * inch, 3.2 * inch, 3.2 * inch, 0.9 * inch, C_BLUE_MED)
    s.c.setFont("Helvetica-Bold", 11)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(4.2 * inch, y_flow + 0.05 * inch, "AI-Platform")
    s.c.setFont("Helvetica", 9)
    s.c.drawCentredString(4.2 * inch, y_flow - 0.2 * inch, "30-90 min automated")

    s.c.setFont("Helvetica-Bold", 16)
    s.c.setFillColor(C_BLUE_MED)
    s.c.drawString(6.0 * inch, y_flow, "=>")

    s.rect(6.65 * inch, 3.2 * inch, 1.8 * inch, 0.9 * inch, C_GREEN)
    s.c.setFont("Helvetica-Bold", 10)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(7.55 * inch, y_flow + 0.05 * inch, "OUTPUT")
    s.c.setFont("Helvetica", 9)
    s.c.drawCentredString(7.55 * inch, y_flow - 0.2 * inch, "Design+N+Synopsis")

    s.slide_number(3, 9, C_BLUE_DARK)


def draw_architecture_slide(c):
    s = Slide(c)
    s.background(C_WHITE)
    s.header_bar("Platform Architecture", C_BLUE_DARK, font_size=22, height=0.65 * inch)

    # Frontend box
    s.rect(0.2 * inch, 0.75 * inch, 3.2 * inch, 4.2 * inch, HexColor("#E3F2FD"))
    s.c.setFont("Helvetica-Bold", 13)
    s.c.setFillColor(C_BLUE_DARK)
    s.c.drawCentredString(1.8 * inch, PAGE_H - 0.75 * inch - 0.35 * inch, "FRONTEND")
    s.c.setFont("Helvetica", 10)
    s.c.drawCentredString(1.8 * inch, PAGE_H - 0.75 * inch - 0.62 * inch, "(React + TypeScript)")

    fe_items = ["Calculator Form", "Results Panel", "AI Chat (RAG)", "Synopsis Export (MD/JSON)"]
    for i, item in enumerate(fe_items):
        s.c.setFont("Helvetica", 11)
        s.c.setFillColor(C_BLACK)
        s.c.drawString(0.5 * inch, PAGE_H - 1.35 * inch - i * 0.45 * inch, f"• {item}")

    # Arrow
    s.c.setFont("Helvetica-Bold", 14)
    s.c.setFillColor(HexColor("#9E9E9E"))
    s.c.drawCentredString(4.1 * inch, PAGE_H - 2.5 * inch, "<=>")
    s.c.setFont("Helvetica", 9)
    s.c.drawCentredString(4.1 * inch, PAGE_H - 2.8 * inch, "HTTP REST")

    # Backend box
    s.rect(4.7 * inch, 0.75 * inch, 4.8 * inch, 4.2 * inch, HexColor("#E8F5E9"))
    s.c.setFont("Helvetica-Bold", 13)
    s.c.setFillColor(C_GREEN)
    s.c.drawCentredString(7.1 * inch, PAGE_H - 0.75 * inch - 0.35 * inch, "BACKEND")
    s.c.setFont("Helvetica", 10)
    s.c.setFillColor(C_GREEN)
    s.c.drawCentredString(7.1 * inch, PAGE_H - 0.75 * inch - 0.62 * inch, "(Kotlin + Quarkus 3.8)")

    modules = [
        (C_BLUE_MED, "Design+N Engine", "DesignEngine.kt · SampleSizeEngine.kt"),
        (C_PURPLE, "RSABE Module", "BECalculationService.kt (Owen's method)"),
        (C_TEAL, "PK Intelligence", "washout · accumulation · t1/2 logic"),
        (C_ACCENT_ORANGE, "Regulatory RAG", "GigaChat + EEC No.85 / EMA / FDA"),
    ]

    y_mod = 1.4 * inch
    for color, title, detail in modules:
        s.rect(4.8 * inch, y_mod, 4.6 * inch, 0.52 * inch, color)
        s.c.setFont("Helvetica-Bold", 10)
        s.c.setFillColor(C_WHITE)
        s.c.drawString(5.0 * inch, PAGE_H - y_mod - 0.22 * inch, title)
        s.c.setFont("Helvetica", 9)
        s.c.drawString(5.0 * inch, PAGE_H - y_mod - 0.42 * inch, detail)
        y_mod += 0.67 * inch

    # Database
    s.rect(4.8 * inch, y_mod + 0.1 * inch, 2.0 * inch, 0.45 * inch, HexColor("#FFF3E0"))
    s.c.setFont("Helvetica-Bold", 11)
    s.c.setFillColor(C_ACCENT_ORANGE)
    s.c.drawCentredString(5.8 * inch, PAGE_H - y_mod - 0.1 * inch - 0.28 * inch, "PostgreSQL DB")

    # Decision flow
    s.rect(0.2 * inch, PAGE_H - 0.6 * inch, PAGE_W - 0.4 * inch, 0.45 * inch, HexColor("#F5F5F5"))
    s.c.setFont("Helvetica", 9)
    s.c.setFillColor(C_BLUE_DARK)
    s.c.drawString(0.35 * inch, PAGE_H - 0.6 * inch - 0.28 * inch,
                   "Flow: CV_intra <=30% => 2x2  |  >30% => RSABE => replicate/parallel => washout => fasting/fed => N => Synopsis")

    s.slide_number(4, 9, C_BLUE_DARK)


def draw_usecase_slide(c):
    s = Slide(c)
    s.background(C_GRAY_LIGHT)
    s.header_bar("Example: High-Variability Oncology Drug", C_TEAL, font_size=20)

    # Input box
    s.rect(0.25 * inch, 0.8 * inch, 3.8 * inch, 2.5 * inch, C_WHITE)
    s.c.setFont("Helvetica-Bold", 12)
    s.c.setFillColor(C_TEAL)
    s.c.drawCentredString(2.15 * inch, PAGE_H - 0.8 * inch - 0.32 * inch, "INPUT PARAMETERS")

    params = [
        ("CV_intra:", "42%", C_ACCENT_RED, "=> High variability (RSABE)"),
        ("t1/2:", "48h", C_ACCENT_RED, "=> Long washout required"),
        ("Food Effect:", "Confirmed", C_ACCENT_RED, "=> 2 studies: fasting + fed"),
    ]
    y_param = 1.25 * inch
    for label, value, val_color, note in params:
        s.c.setFont("Helvetica", 11)
        s.c.setFillColor(C_BLACK)
        s.c.drawString(0.45 * inch, PAGE_H - y_param - 0.22 * inch, label)
        s.c.setFont("Helvetica-Bold", 12)
        s.c.setFillColor(val_color)
        s.c.drawString(1.35 * inch, PAGE_H - y_param - 0.22 * inch, value)
        s.c.setFont("Helvetica-Oblique", 9)
        s.c.setFillColor(C_GRAY_MED)
        s.c.drawString(0.45 * inch, PAGE_H - y_param - 0.48 * inch, note)
        y_param += 0.68 * inch

    # Arrow
    s.c.setFont("Helvetica-Bold", 22)
    s.c.setFillColor(C_BLUE_MED)
    s.c.drawCentredString(4.4 * inch, PAGE_H - 2.1 * inch, "=>")

    # Output box
    s.rect(4.85 * inch, 0.8 * inch, 3.8 * inch, 2.5 * inch, HexColor("#E8F5E9"))
    s.c.setFont("Helvetica-Bold", 12)
    s.c.setFillColor(C_GREEN)
    s.c.drawCentredString(6.75 * inch, PAGE_H - 0.8 * inch - 0.32 * inch, "PLATFORM RESULTS")

    results = [
        "Study Design: 4-period replicate",
        "Studies: 2 (fasting + fed)",
        "N randomized: 64",
        "N screening: 92",
        "Synopsis: auto-generated",
    ]
    for i, result in enumerate(results):
        s.c.setFont("Helvetica", 11)
        s.c.setFillColor(C_BLACK)
        s.c.drawString(5.05 * inch, PAGE_H - 1.25 * inch - i * 0.42 * inch, f"[v] {result}")

    # Time reduction bars
    s.c.setFont("Helvetica-Bold", 12)
    s.c.setFillColor(C_BLUE_DARK)
    s.c.drawString(0.25 * inch, PAGE_H - 3.55 * inch, "Time Reduction Comparison:")

    s.c.setFont("Helvetica", 10)
    s.c.setFillColor(C_BLACK)
    s.c.drawString(0.25 * inch, PAGE_H - 3.9 * inch, "Manual planning (2-5 days):")
    s.rect(2.9 * inch, 3.75 * inch, 5.0 * inch, 0.28 * inch, C_ACCENT_RED)

    s.c.drawString(0.25 * inch, PAGE_H - 4.28 * inch, "AI-Platform (30-90 min):")
    s.rect(2.9 * inch, 4.13 * inch, 1.8 * inch, 0.28 * inch, C_GREEN)

    s.c.setFont("Helvetica-Bold", 10)
    s.c.setFillColor(HexColor("#BDBDBD"))
    s.c.drawString(5.0 * inch, PAGE_H - 4.28 * inch, "~65% faster")

    # Summary badge
    s.rect(0.2 * inch, PAGE_H - 0.7 * inch, PAGE_W - 0.4 * inch, 0.52 * inch, C_TEAL)
    s.c.setFont("Helvetica-Bold", 12)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(PAGE_W / 2, PAGE_H - 0.44 * inch,
                          "60-70% time savings  *  Eliminate manual errors  *  Regulatory-ready output in minutes")

    s.slide_number(5, 9, C_BLUE_DARK)


def draw_competitive_slide(c):
    s = Slide(c)
    s.background(C_WHITE)
    s.header_bar("Competitive Advantages", C_BLUE_DARK)

    headers = ["Solution Type", "Limitation", "Our Advantage"]
    header_colors = [C_BLUE_MED, C_ACCENT_RED, C_GREEN]
    col_w = [2.5 * inch, 2.8 * inch, 3.2 * inch]
    col_x = [0.2 * inch, 2.75 * inch, 5.6 * inch]
    row_h = 0.45 * inch

    y_hdr = 0.75 * inch
    for i, (hdr, color, w, x) in enumerate(zip(headers, header_colors, col_w, col_x)):
        s.rect(x, y_hdr, w, row_h, color)
        s.c.setFont("Helvetica-Bold", 11)
        s.c.setFillColor(C_WHITE)
        s.c.drawCentredString(x + w / 2, PAGE_H - y_hdr - 0.28 * inch, hdr)

    rows = [
        ("Manual calculation\n(Excel/SAS)",
         "Human errors\nNot regulatory-aware\n2-5 days per study",
         "[v] Automated engine\n[v] EMA/FDA compliant\n[v] 30-90 min"),
        ("Generic BE software\n(WinNonlin, Phoenix)",
         "No oncology logic\nNo regulatory AI\nExpensive licenses",
         "[v] Onco-specific RSABE\n[v] Built-in RAG\n[v] Cloud SaaS model"),
        ("CRO consulting\n(manual expert work)",
         "High cost per study\nNon-reproducible\nSlow turnaround",
         "[v] Deterministic core\n[v] Reproducible results\n[v] Audit trail"),
    ]

    row_colors = [HexColor("#F8F9FA"), HexColor("#F1F3F4"), HexColor("#E8EAED")]
    y_row = 1.25 * inch
    content_row_h = 1.1 * inch

    for row_idx, (sol, lim, adv) in enumerate(rows):
        bg = row_colors[row_idx]
        for i, (text, w, x) in enumerate(zip([sol, lim, adv], col_w, col_x)):
            s.rect(x, y_row, w, content_row_h, bg)
            lines = text.split("\n")
            for j, line in enumerate(lines):
                font = "Helvetica-Bold" if j == 0 and i == 0 else "Helvetica"
                color = C_BLUE_DARK if j == 0 else C_BLACK
                if i == 2:
                    color = C_GREEN
                s.c.setFont(font, 10)
                s.c.setFillColor(color)
                s.c.drawString(x + 0.1 * inch, PAGE_H - y_row - 0.25 * inch - j * 0.28 * inch, line)
        y_row += content_row_h + 0.05 * inch

    # Bottom summary
    s.rect(0.2 * inch, PAGE_H - 0.65 * inch, PAGE_W - 0.4 * inch, 0.52 * inch, C_BLUE_DARK)
    s.c.setFont("Helvetica-Bold", 12)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(PAGE_W / 2, PAGE_H - 0.4 * inch,
                          "We combine statistics + regulatory expertise + AI explanations in one platform")

    s.slide_number(6, 9, C_BLUE_DARK)


def draw_investment_slide(c):
    s = Slide(c)
    s.background(C_BLUE_DARK)

    s.rect(0, 0, PAGE_W, 0.06 * inch, C_GREEN)

    s.c.setFont("Helvetica-Bold", 28)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(PAGE_W / 2, PAGE_H - 0.65 * inch, "Investment Logic")

    blocks = [
        (C_BLUE_MED, "BE is Standardized", "=> Algorithmic\n\nHighly regulated domain\nwith deterministic rules\n=> perfect for automation"),
        (C_PURPLE, "Oncology is Complex", "=> High Automation Value\n\nHigh-variability drugs\nrequire expert-level logic\n=> AI provides leverage"),
        (C_GREEN, "AI Infrastructure Layer", "Not just a calculator\n\nFull regulatory stack:\nRAG + RSABE + PK +\nSynopsis generation"),
    ]

    block_w = 2.8 * inch
    for i, (color, title, desc) in enumerate(blocks):
        x = 0.3 * inch + i * (block_w + 0.15 * inch)
        s.rect(x, 0.85 * inch, block_w, 0.55 * inch, color)
        s.c.setFont("Helvetica-Bold", 11)
        s.c.setFillColor(C_WHITE)
        s.c.drawCentredString(x + block_w / 2, PAGE_H - 0.85 * inch - 0.32 * inch, title)
        s.rect(x, 1.45 * inch, block_w, 1.9 * inch, HexColor("#1E2A40"))
        lines = desc.split("\n")
        for j, line in enumerate(lines):
            font = "Helvetica-Bold" if j == 0 else "Helvetica"
            s.c.setFont(font, 10)
            s.c.setFillColor(C_WHITE)
            s.c.drawString(x + 0.1 * inch, PAGE_H - 1.45 * inch - 0.25 * inch - j * 0.32 * inch, line)

    # ROI section
    s.c.setFont("Helvetica-Bold", 14)
    s.c.setFillColor(C_GREEN)
    s.c.drawString(0.3 * inch, PAGE_H - 3.65 * inch, "ROI / Time Savings")

    roi_data = [
        ("Savings per study:", "~88,000 RUB"),
        ("30 studies/year:", "~2.6M RUB saved"),
        ("Implementation cost:", "~3M RUB"),
        ("Breakeven:", "~12 months"),
        ("5-year ROI:", "400-600%"),
    ]

    for i, (label, value) in enumerate(roi_data):
        y = PAGE_H - 4.05 * inch - i * 0.35 * inch
        s.c.setFont("Helvetica", 11)
        s.c.setFillColor(HexColor("#BDBDBD"))
        s.c.drawString(0.3 * inch, y, label)
        s.c.setFont("Helvetica-Bold", 12)
        s.c.setFillColor(C_GREEN)
        s.c.drawString(2.8 * inch, y, value)

    # Growth arrow symbol
    s.c.setFont("Helvetica-Bold", 60)
    s.c.setFillColor(HexColor("#1B5E20"))
    s.c.drawCentredString(7.0 * inch, PAGE_H - 5.0 * inch, "^")
    s.c.setFont("Helvetica-Bold", 14)
    s.c.setFillColor(C_GREEN)
    s.c.drawCentredString(7.0 * inch, PAGE_H - 5.6 * inch, "GROWTH")

    s.slide_number(7, 9)


def draw_economics_slide(c):
    s = Slide(c)
    s.background(C_WHITE)
    s.header_bar("Russian BE Market — Economics", C_TEAL, font_size=20)

    companies = [
        ("Synergy Research Group", "900-1,200M RUB/year"),
        ("Medical Development Agency", "290-380M RUB/year"),
        ("iPharma", "360-480M RUB/year"),
        ("CPD CRO", "180-240M RUB/year"),
        ("QBio Research", "96-128M RUB/year"),
        ("OCT CRO", "165-220M RUB/year"),
        ("R&D Pharma", "72-96M RUB/year"),
    ]

    # Table header
    s.rect(0.2 * inch, 0.75 * inch, 4.5 * inch, 0.4 * inch, C_TEAL)
    s.c.setFont("Helvetica-Bold", 11)
    s.c.setFillColor(C_WHITE)
    s.c.drawString(0.35 * inch, PAGE_H - 0.75 * inch - 0.25 * inch, "CRO Company")

    s.rect(4.75 * inch, 0.75 * inch, 3.8 * inch, 0.4 * inch, C_TEAL)
    s.c.drawCentredString(6.65 * inch, PAGE_H - 0.75 * inch - 0.25 * inch, "Revenue/Year (RUB)")

    row_colors = [HexColor("#F0FDF4"), HexColor("#E0F7F4")]
    y_row = 1.2 * inch
    for i, (company, rev) in enumerate(companies):
        bg = row_colors[i % 2]
        s.rect(0.2 * inch, y_row, 4.5 * inch, 0.4 * inch, bg)
        s.c.setFont("Helvetica", 11)
        s.c.setFillColor(C_BLACK)
        s.c.drawString(0.35 * inch, PAGE_H - y_row - 0.25 * inch, company)
        s.rect(4.75 * inch, y_row, 3.8 * inch, 0.4 * inch, bg)
        s.c.setFont("Helvetica-Bold", 11)
        s.c.setFillColor(C_TEAL)
        s.c.drawCentredString(6.65 * inch, PAGE_H - y_row - 0.25 * inch, rev)
        y_row += 0.43 * inch

    # Cost comparison
    s.c.setFont("Helvetica-Bold", 13)
    s.c.setFillColor(C_BLUE_DARK)
    s.c.drawString(0.2 * inch, PAGE_H - 4.45 * inch, "Cost per BE Study:")

    comparisons = [
        ("Manual planning (expert CRO):", "96,000 RUB", C_ACCENT_RED),
        ("AI-Platform (automated):", "8,000 RUB", C_GREEN),
        ("Savings per study:", "88,000 RUB", C_TEAL),
    ]
    for i, (label, value, color) in enumerate(comparisons):
        y = PAGE_H - 4.85 * inch - i * 0.38 * inch
        s.c.setFont("Helvetica", 11)
        s.c.setFillColor(C_BLACK)
        s.c.drawString(0.2 * inch, y, label)
        s.c.setFont("Helvetica-Bold", 12)
        s.c.setFillColor(color)
        s.c.drawString(3.5 * inch, y, value)

    s.slide_number(8, 9, C_BLUE_DARK)


def draw_team_slide(c):
    s = Slide(c)
    s.background(C_BLUE_DARK)

    s.rect(0, 0, PAGE_W, 0.06 * inch, C_ACCENT_ORANGE)

    s.c.setFont("Helvetica-Bold", 28)
    s.c.setFillColor(C_WHITE)
    s.c.drawCentredString(PAGE_W / 2, PAGE_H - 0.65 * inch, "Team")

    members = [
        ("1", "Clinical Pharmacologist", "BE design, PK analysis,\nregulatory submissions"),
        ("2", "Backend Engineer", "Kotlin/Quarkus,\ncalculation engines, APIs"),
        ("3", "Frontend Developer", "React/TypeScript,\nUX/UI, calculator"),
        ("4", "AI/ML Engineer", "GigaChat RAG, NLP,\nexplanation layer"),
    ]

    member_w = 2.1 * inch
    for i, (num, role, skills) in enumerate(members):
        x = 0.3 * inch + i * (member_w + 0.1 * inch)
        # Circle avatar
        s.rect(x + 0.35 * inch, 0.85 * inch, 1.4 * inch, 1.4 * inch, C_BLUE_MED)
        s.c.setFont("Helvetica-Bold", 24)
        s.c.setFillColor(C_WHITE)
        s.c.drawCentredString(x + 1.05 * inch, PAGE_H - 0.85 * inch - 0.8 * inch, num)
        # Role
        s.c.setFont("Helvetica-Bold", 11)
        s.c.setFillColor(C_WHITE)
        lines = role.split(" ")
        # Simple wrapping
        if len(role) > 20:
            mid = len(role) // 2
            split_idx = role.rfind(" ", 0, mid)
            line1 = role[:split_idx]
            line2 = role[split_idx + 1:]
            s.c.drawCentredString(x + 1.05 * inch, PAGE_H - 2.5 * inch, line1)
            s.c.drawCentredString(x + 1.05 * inch, PAGE_H - 2.8 * inch, line2)
        else:
            s.c.drawCentredString(x + 1.05 * inch, PAGE_H - 2.5 * inch, role)
        # Skills
        s.c.setFont("Helvetica", 9)
        s.c.setFillColor(HexColor("#BDBDBD"))
        for j, skill_line in enumerate(skills.split("\n")):
            s.c.drawCentredString(x + 1.05 * inch, PAGE_H - 3.1 * inch - j * 0.3 * inch, skill_line)

    # Links section
    s.c.setFont("Helvetica-Bold", 13)
    s.c.setFillColor(C_ACCENT_ORANGE)
    s.c.drawString(0.3 * inch, PAGE_H - 4.0 * inch, "Project Links:")

    links = [
        ("Repository:", "github.com/medmancifra/sendbox"),
        ("Issue #5:", "github.com/medmancifra/sendbox/issues/5"),
        ("PR #6:", "github.com/medmancifra/sendbox/pull/6"),
    ]
    for i, (label, url) in enumerate(links):
        s.c.setFont("Helvetica", 11)
        s.c.setFillColor(HexColor("#BDBDBD"))
        s.c.drawString(0.3 * inch, PAGE_H - 4.45 * inch - i * 0.35 * inch, f"{label}  {url}")

    # Bottom bar
    s.rect(0, PAGE_H - 0.5 * inch, PAGE_W, 0.5 * inch, C_PURPLE)
    s.c.setFont("Helvetica", 10)
    s.c.setFillColor(HexColor("#BDBDBD"))
    s.c.drawCentredString(PAGE_W / 2, PAGE_H - 0.32 * inch,
                          "AI-Platform for BE Research (Oncology)  *  2024  *  Made with love + code")

    s.slide_number(9, 9)


def main():
    output_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                              "docs", "presentation")
    os.makedirs(output_dir, exist_ok=True)

    pdf_path = os.path.join(output_dir, "BE_AI_Platform_Presentation.pdf")

    c = canvas.Canvas(pdf_path, pagesize=landscape(A4))
    c.setTitle("AI-Platform for Automating Bioequivalence Research (Oncology)")
    c.setAuthor("konard / medmancifra")
    c.setSubject("Hackathon / Investment Pitch Presentation")

    slides = [
        ("Slide 1: Title", draw_title_slide),
        ("Slide 2: Problem", draw_problem_slide),
        ("Slide 3: Solution", draw_solution_slide),
        ("Slide 4: Architecture", draw_architecture_slide),
        ("Slide 5: Use Case", draw_usecase_slide),
        ("Slide 6: Competitive Advantages", draw_competitive_slide),
        ("Slide 7: Investment Logic", draw_investment_slide),
        ("Slide 8: Economics", draw_economics_slide),
        ("Slide 9: Team", draw_team_slide),
    ]

    print("Creating PDF slides...")
    for label, draw_fn in slides:
        draw_fn(c)
        c.showPage()
        print(f"  [v] {label}")

    c.save()
    size_kb = os.path.getsize(pdf_path) / 1024
    print(f"\n[v] PDF saved: {pdf_path}")
    print(f"    File size: {size_kb:.1f} KB")
    return pdf_path


if __name__ == "__main__":
    main()
