use anchor_lang::prelude::*;

// IMPORTANT: Replace this with your actual program ID after running:
//   solana-keygen new -o target/deploy/solaria_program-keypair.json
//   solana address -k target/deploy/solaria_program-keypair.json
// Then paste the output here AND in Anchor.toml.
declare_id!("Fg6PaFpoGXkYsidMpWTK6W2BeZ7FEfcYkg476zPFsLnS");

#[program]
pub mod solaria_program {
    use super::*;

    /// Creates a new PaymentIntent on-chain.
    /// Called by the merchant to generate a payment link.
    pub fn create_payment_intent(
        ctx: Context<CreatePaymentIntent>,
        amount: u64,
        description: String,
        intent_id: u64,
    ) -> Result<()> {
        require!(amount > 0, SolariaError::InvalidAmount);
        require!(description.len() <= 140, SolariaError::DescriptionTooLong);

        let intent = &mut ctx.accounts.payment_intent;
        intent.intent_id = intent_id;
        intent.merchant = ctx.accounts.merchant.key();
        intent.amount = amount;
        intent.status = PaymentStatus::Pending;
        intent.description = description;
        intent.settled_source_chain_tx = "".to_string();
        intent.settled_proof_hash = [0u8; 32];

        emit!(PaymentIntentCreated {
            intent_id,
            merchant: intent.merchant,
            amount,
        });

        Ok(())
    }

    /// Settles a PaymentIntent after cross-chain payment is verified.
    /// Only callable by the configured oracle.
    pub fn settle_payment(
        ctx: Context<SettlePayment>,
        intent_id: u64,
        source_chain_tx: String,
        proof_hash: [u8; 32],
    ) -> Result<()> {
        require!(source_chain_tx.len() <= 200, SolariaError::SourceTxTooLong);
        require!(!source_chain_tx.is_empty(), SolariaError::SourceTxEmpty);

        let intent = &mut ctx.accounts.payment_intent;
        require!(intent.intent_id == intent_id, SolariaError::IntentIdMismatch);
        require!(
            intent.status == PaymentStatus::Pending,
            SolariaError::AlreadySettled
        );

        // Oracle-gated settlement: only the configured oracle pubkey may call.
        require_keys_eq!(
            ctx.accounts.oracle.key(),
            ctx.accounts.config.oracle,
            SolariaError::UnauthorizedOracle
        );

        intent.status = PaymentStatus::Settled;
        intent.settled_source_chain_tx = source_chain_tx;
        intent.settled_proof_hash = proof_hash;

        emit!(PaymentIntentSettled {
            intent_id,
            merchant: intent.merchant,
        });

        Ok(())
    }

    /// Initializes the global config PDA. Can only be called once.
    pub fn init_config(ctx: Context<InitConfig>, oracle: Pubkey) -> Result<()> {
        let cfg = &mut ctx.accounts.config;
        cfg.admin = ctx.accounts.admin.key();
        cfg.oracle = oracle;
        Ok(())
    }

    /// Updates the oracle pubkey. Only callable by the admin.
    pub fn set_oracle(ctx: Context<SetOracle>, oracle: Pubkey) -> Result<()> {
        let cfg = &mut ctx.accounts.config;
        require_keys_eq!(
            cfg.admin,
            ctx.accounts.admin.key(),
            SolariaError::UnauthorizedAdmin
        );
        cfg.oracle = oracle;
        Ok(())
    }
}

// ---------------------------------------------------------------------------
// Account structs (Anchor derives validation + deserialization)
// ---------------------------------------------------------------------------

#[derive(Accounts)]
#[instruction(amount: u64, description: String, intent_id: u64)]
pub struct CreatePaymentIntent<'info> {
    #[account(mut)]
    pub merchant: Signer<'info>,

    #[account(
        init,
        payer = merchant,
        space = 8 + PaymentIntent::MAX_SIZE,
        seeds = [b"intent", merchant.key().as_ref(), &intent_id.to_le_bytes()],
        bump
    )]
    pub payment_intent: Account<'info, PaymentIntent>,

    pub system_program: Program<'info, System>,
}

#[derive(Accounts)]
pub struct SettlePayment<'info> {
    /// Oracle signer — verified against config.oracle in the handler.
    pub oracle: Signer<'info>,

    #[account(
        mut,
        seeds = [b"intent", payment_intent.merchant.as_ref(), &payment_intent.intent_id.to_le_bytes()],
        bump
    )]
    pub payment_intent: Account<'info, PaymentIntent>,

    #[account(
        seeds = [b"config"],
        bump
    )]
    pub config: Account<'info, SolariaConfig>,
}

#[derive(Accounts)]
pub struct InitConfig<'info> {
    #[account(mut)]
    pub admin: Signer<'info>,

    #[account(
        init,
        payer = admin,
        space = 8 + SolariaConfig::SIZE,
        seeds = [b"config"],
        bump
    )]
    pub config: Account<'info, SolariaConfig>,

    pub system_program: Program<'info, System>,
}

#[derive(Accounts)]
pub struct SetOracle<'info> {
    pub admin: Signer<'info>,

    #[account(
        mut,
        seeds = [b"config"],
        bump
    )]
    pub config: Account<'info, SolariaConfig>,
}

// ---------------------------------------------------------------------------
// Data accounts
// ---------------------------------------------------------------------------

#[account]
pub struct SolariaConfig {
    pub admin: Pubkey,
    pub oracle: Pubkey,
}
impl SolariaConfig {
    pub const SIZE: usize = 32 + 32;
}

#[account]
pub struct PaymentIntent {
    pub intent_id: u64,
    pub merchant: Pubkey,
    pub amount: u64,
    pub status: PaymentStatus,
    pub description: String,
    pub settled_source_chain_tx: String,
    pub settled_proof_hash: [u8; 32],
}
impl PaymentIntent {
    // Conservative sizing: Anchor string = 4 + bytes
    pub const MAX_SIZE: usize =
        8 +       // intent_id
        32 +      // merchant
        8 +       // amount
        1 +       // status enum
        (4 + 140) + // description
        (4 + 200) + // source tx
        32;       // proof hash
}

#[derive(AnchorSerialize, AnchorDeserialize, Clone, PartialEq, Eq)]
pub enum PaymentStatus {
    Pending,
    Settled,
}

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

#[event]
pub struct PaymentIntentCreated {
    pub intent_id: u64,
    pub merchant: Pubkey,
    pub amount: u64,
}

#[event]
pub struct PaymentIntentSettled {
    pub intent_id: u64,
    pub merchant: Pubkey,
}

// ---------------------------------------------------------------------------
// Errors
// ---------------------------------------------------------------------------

#[error_code]
pub enum SolariaError {
    #[msg("Description too long (max 140 chars)")]
    DescriptionTooLong,
    #[msg("Source chain tx string too long (max 200 chars)")]
    SourceTxTooLong,
    #[msg("Source chain tx must not be empty")]
    SourceTxEmpty,
    #[msg("Intent ID mismatch")]
    IntentIdMismatch,
    #[msg("Payment intent already settled")]
    AlreadySettled,
    #[msg("Unauthorized oracle signer")]
    UnauthorizedOracle,
    #[msg("Unauthorized admin signer")]
    UnauthorizedAdmin,
    #[msg("Amount must be greater than zero")]
    InvalidAmount,
}
