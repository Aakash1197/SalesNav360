import {
  ArrowRight,
  BadgeCheck,
  ChartNoAxesCombined,
  ClipboardCheck,
  ExternalLink,
  FileSearch,
  HelpCircle,
  Instagram,
  HandCoins,
  Mail,
  MapPin,
  MessageCircle,
  Phone,
  ShieldCheck,
} from 'lucide-react';
import { Link } from 'react-router-dom';

const services = [
  {
    icon: FileSearch,
    title: 'Loan rejection assistance',
    description: 'Review a lender’s response and identify questions or application details worth clarifying.',
  },
  {
    icon: ChartNoAxesCombined,
    title: 'Credit profile guidance',
    description: 'Understand the information in your credit profile and the factors a lender may consider.',
  },
  {
    icon: ClipboardCheck,
    title: 'Loan eligibility assessment',
    description: 'Organize your details and discuss how they may relate to common lender requirements.',
  },
  {
    icon: HandCoins,
    title: 'Alternative loan options',
    description: 'Explore options to research and questions to ask before approaching another lender.',
  },
  {
    icon: MessageCircle,
    title: 'Financial consultation',
    description: 'Talk through your circumstances and leave with clearer, practical next steps.',
  },
];

const faqs = [
  {
    question: 'Why might a loan application be rejected?',
    answer: 'Lenders may consider income, existing obligations, credit history, documentation, and their own eligibility policies. The reason varies by lender and application.',
  },
  {
    question: 'What should I do after a loan rejection?',
    answer: 'Review any explanation from the lender, ask them what information they can share, and check your application details for errors before deciding what to do next.',
  },
  {
    question: 'Can I apply again straight away?',
    answer: 'That depends on the lender and your circumstances. It can be useful to understand the reason for the decision first and ask the lender whether they recommend waiting.',
  },
  {
    question: 'Can you guarantee loan approval?',
    answer: 'No. Only a lender can assess an application and make a lending decision. We provide general guidance and cannot guarantee approval, rates, or terms.',
  },
];

function handleContactSubmit(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const subject = `Consultation request from ${formData.get('name')}`;
  const body = [
    `Name: ${formData.get('name')}`,
    `Email: ${formData.get('email')}`,
    '',
    'Message:',
    formData.get('message'),
  ].join('\n');

  window.location.href = `mailto:jayinfotech20@gmail.com?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
}

export default function LandingPage() {
  return (
    <main className="public-site">
      <div
        className="public-site-background"
        aria-hidden="true"
        style={{ backgroundImage: "url('/loan-advisor-background.png')" }}
      />
      <header className="public-header">
        <a className="public-brand" href="#home" aria-label="Apex Finodeal LLP home">
          <img src="/apex-finodeal-mark.svg" alt="" />
          <span><strong>Apex Finodeal LLP</strong><small>FINANCIAL GUIDANCE</small></span>
        </a>
        <nav className="public-nav" aria-label="Main navigation">
          <a href="#home">Home</a>
          <a href="#about">About Us</a>
          <a href="#services">Services</a>
          <a href="#contact">Contact Us</a>
        </nav>
        <Link className="public-login" to="/login">Login <ArrowRight size={16} /></Link>
      </header>

      <section className="public-hero" id="home">
        <div className="public-hero-inner">
          <div className="public-hero-copy">
            <span className="public-eyebrow"><span /> SUPPORT AFTER A LOAN DECLINE</span>
            <h1>Loan Rejected?<br /><em>Let&apos;s Explore Your Options.</em></h1>
            <p>A rejected loan application doesn&apos;t mean your options end here. Explore financial guidance, understand eligibility requirements, and discover possible next steps.</p>
            <div className="public-hero-actions">
              <a className="public-primary-link" href="#services">Explore Services <ArrowRight size={17} /></a>
              <Link className="public-primary-link" to="/login">Login <ArrowRight size={17} /></Link>
            </div>
            <div className="public-reassurance"><ShieldCheck size={17} /><span>Clear information. Thoughtful guidance. No promises of approval.</span></div>
          </div>
          <div className="public-hero-art">
            <img src="/loan-advisor-background.png" alt="A bank advisor explaining loan information to a customer couple" />
            <div className="public-art-note"><span className="public-art-note-icon"><ChartNoAxesCombined size={18} /></span><span><strong>A clearer way forward</strong><small>One step at a time</small></span><ArrowRight size={16} /></div>
            <span className="public-art-caption">A MORE INFORMED NEXT STEP</span>
          </div>
        </div>
        <div className="public-hero-bottom"><span>GUIDANCE BUILT AROUND YOUR SITUATION</span><span>01 — 05</span></div>
      </section>

      <section className="public-about" id="about">
        <div className="public-about-mark" aria-hidden="true"><BadgeCheck size={29} /></div>
        <div className="public-about-copy">
          <span className="public-section-kicker">ABOUT US</span>
          <h2>Support to help you<br /><em>see the way forward.</em></h2>
        </div>
        <div className="public-about-detail">
          <p>At Apex Finodeal LLP, we help people make sense of a loan decision and consider informed next steps. We listen to your situation, help you organize the details, and offer guidance without promising an outcome.</p>
          <p>Every lender makes its own decisions. Our role is to help you feel better prepared for the questions and choices ahead.</p>
        </div>
      </section>

      <section className="public-services" id="services">
        <div className="public-section-intro">
          <div><span className="public-section-kicker">HOW WE CAN HELP</span><h2>Make the next move<br /><em>with more clarity.</em></h2></div>
          <p>A declined application can leave you with more questions than answers. We help you look at the details and prepare for an informed conversation about what comes next.</p>
        </div>
        <div className="public-service-grid">
          {services.map(({ icon: Icon, title, description }, index) => (
            <article className="public-service-card" id={`service-${index + 1}`} key={title}>
              <div className="public-service-card-top"><span className="public-service-icon"><Icon size={20} /></span><span>{String(index + 1).padStart(2, '0')}</span></div>
              <h3>{title}</h3>
              <p>{description}</p>
              <a href="#contact" aria-label={`Ask about ${title}`}><ArrowRight size={17} /></a>
            </article>
          ))}
        </div>
        <p className="public-disclaimer">Guidance is general and depends on the information you share. Lenders set their own eligibility criteria and make all lending decisions. Approval, rates, and terms are not guaranteed.</p>
      </section>

      <section className="public-approach" id="how-it-works">
        <div className="public-approach-heading">
          <span className="public-section-kicker">HOW IT WORKS</span>
          <h2>Start with a conversation.<br /><em>Then take it one step at a time.</em></h2>
        </div>
        <div className="public-steps">
          <article><span>01</span><div><h3>Tell us what happened</h3><p>Share what you are comfortable sharing about the application and the response you received.</p></div></article>
          <article><span>02</span><div><h3>Review what matters</h3><p>We help you organize the details and identify questions or documents to follow up on.</p></div></article>
          <article><span>03</span><div><h3>Choose your next step</h3><p>Leave with clearer information to help you decide what to explore next. The choice stays yours.</p></div></article>
          <article><span>04</span><div><h3>Stay in control</h3><p>Take the time you need to compare information and make a decision that suits your circumstances.</p></div></article>
        </div>
      </section>

      <section className="public-faq" id="faqs">
        <div className="public-faq-heading">
          <span className="public-section-kicker"><HelpCircle size={15} /> QUICK ANSWERS</span>
          <h2>Questions after a<br /><em>loan decision?</em></h2>
          <p>Every lender and application is different. Here are a few general points to help you start.</p>
        </div>
        <div className="public-faq-list">
          {faqs.map(({ question, answer }) => (
            <details key={question}>
              <summary>{question}<span aria-hidden="true">+</span></summary>
              <p>{answer}</p>
            </details>
          ))}
        </div>
      </section>

      <section className="public-contact" id="contact">
        <div className="public-contact-copy">
          <span className="public-section-kicker">LET’S TALK</span>
          <h2>Your next step<br /><em>starts here.</em></h2>
          <p>Visit a branch or reach out through your preferred channel. We’ll listen, explain how we may be able to help, and let you decide what feels right.</p>
        </div>

        <div className="public-contact-details">
          <article className="public-contact-info-card">
            <span className="public-contact-info-icon"><MapPin size={20} /></span>
            <div>
              <h3>Office information</h3>
              <p><strong>Branch 1</strong>Bhawani Path, Madal Wasti, Solapur - 413002</p>
              <p><strong>Branch 2</strong>Thobade Mala, Degaon Road, Behind Reliance Market - 413001</p>
            </div>
          </article>
          <article className="public-contact-info-card">
            <span className="public-contact-info-icon public-contact-phone-icon"><Phone size={20} /></span>
            <div>
              <h3>Get in touch</h3>
              <a className="public-contact-phone" href="tel:+918669178491">+91 8669178491</a>
              <div className="public-social-links">
                <a className="public-social-whatsapp" href="https://wa.me/918669178491" target="_blank" rel="noopener noreferrer">
                  <MessageCircle size={17} /> WhatsApp
                </a>
                <a className="public-social-instagram" href="https://www.instagram.com/jit_computer_laptop/" target="_blank" rel="noopener noreferrer">
                  <Instagram size={17} /> Instagram
                </a>
              </div>
            </div>
          </article>
          <article className="public-contact-info-card public-contact-email-card">
            <span className="public-contact-info-icon public-contact-email-icon"><Mail size={20} /></span>
            <div>
              <h3>Email support</h3>
              <a href="mailto:jayinfotech20@gmail.com">jayinfotech20@gmail.com</a>
            </div>
          </article>
        </div>

        <div className="public-contact-map">
          <div className="public-map-frame">
            <iframe
              title="Jay Info Tech location in Solapur"
              src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d4509.537188624631!2d75.8852045!3d17.665616200000002!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3bc5d1a3ed04eb51%3A0x5e5e001cb6e2812e!2sJay%20Info%20Tech!5e1!3m2!1sen!2sin!4v1767460314992!5m2!1sen!2sin"
              loading="lazy"
              referrerPolicy="no-referrer-when-downgrade"
              allowFullScreen
            />
          </div>
          <a className="public-map-link" href="https://maps.app.goo.gl/TDTAgpCT62LgY5JM8" target="_blank" rel="noopener noreferrer">
            Open in Google Maps <ExternalLink size={17} />
          </a>
        </div>

        <div className="public-contact-form-card">
          <div>
            <span className="public-contact-card-label">SEND US A MESSAGE</span>
            <p>Your email app will open with your message ready to send.</p>
          </div>
          <form className="public-contact-form" aria-label="Contact form" onSubmit={handleContactSubmit}>
            <label htmlFor="contact-name">Your name</label>
            <input id="contact-name" name="name" autoComplete="name" required />
            <label htmlFor="contact-email">Email address</label>
            <input id="contact-email" name="email" type="email" autoComplete="email" required />
            <label htmlFor="contact-message">How can we help?</label>
            <textarea id="contact-message" name="message" rows="3" required />
            <button type="submit">Continue to email <ArrowRight size={16} /></button>
            <p>Please don’t include passwords or sensitive financial documents.</p>
          </form>
        </div>
      </section>

      <section className="public-legal-notes" aria-label="Privacy and terms">
        <article id="privacy">
          <h2>Privacy policy</h2>
          <p>The contact form opens your email app; it does not send information to this website. Please do not include passwords, account access, or sensitive financial documents.</p>
        </article>
        <article id="terms">
          <h2>Terms of use</h2>
          <p>Information on this page is general guidance, not a lending decision or financial guarantee. Lenders determine eligibility, approval, rates, and terms.</p>
        </article>
      </section>

      <footer className="public-footer">
        <div className="public-footer-main">
          <div className="public-footer-brand">
            <a className="public-brand" href="#home">
              <img src="/apex-finodeal-mark.svg" alt="" />
              <span><strong>Apex Finodeal LLP</strong><small>FINANCIAL GUIDANCE</small></span>
            </a>
            <p>Helping you find clarity after a loan decision.</p>
          </div>
          <nav aria-label="Footer navigation">
            <h2>Explore</h2>
            <a href="#home">Home</a>
            <a href="#about">About Us</a>
            <a href="#how-it-works">How It Works</a>
            <a href="#faqs">FAQs</a>
            <a href="#contact">Contact Us</a>
          </nav>
          <nav aria-label="Footer services">
            <h2>Services</h2>
            {services.map(({ title }, index) => <a href={`#service-${index + 1}`} key={title}>{title}</a>)}
          </nav>
          <nav aria-label="Footer legal links">
            <h2>Information</h2>
            <a href="#privacy">Privacy policy</a>
            <a href="#terms">Terms of use</a>
            <Link to="/login">Login</Link>
          </nav>
        </div>
        <div className="public-footer-bottom">
          <small>© {new Date().getFullYear()} Apex Finodeal LLP. All rights reserved.</small>
          <a href="#home">Back to top ↑</a>
        </div>
      </footer>
    </main>
  );
}
