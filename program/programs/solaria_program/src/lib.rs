use anchor_lang::prelude::*;

declare_id!("So1ar1a1111111111111111111111111111111111");

#[program]
pub mod solaria_program {
    use super::*;

    pub fn create_payment_intent(
        ctx: Context<CreatePaymentIntent>,
        amount: u64,
        description: String,
        intent_id: u64,
    ) -> Result<()> {
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

    pub fn settle_payment(
        ctx: Context<SettlePayment>,
        intent_id: u64,
        source_chain_tx: String,
        proof_hash: [u8; 32],
    ) -> Result<()> {
        require!(source_chain_tx.len() <= 200, SolariaError::SourceTxTooLong);

        let intent = &mut ctx.accounts.payment_intent;
        require!(intent.intent_id == intent_id, SolariaError::IntentIdMismatch);
        require!(intent.status == PaymentStatus::Pending, SolariaError::AlreadySettled);

        // Oracle-gated settlement: only the configured oracle pubkey may call.
        require_keys_eq!(ctx.accounts.oracle.key(), ctx.accounts.config.oracle, SolariaError::UnauthorizedOracle);

        intent.status = PaymentStatus::Settled;
        intent.settled_source_chain_tx = source_chain_tx;
        intent.settled_proof_hash = proof_hash;

        emit!(PaymentIntentSettled {
            intent_id,
            merchant: intent.merchant,
        });

        Ok(())
    }

    pub fn init_config(ctx: Context<InitConfig>, oracle: Pubkey) -> Result<()> {
        let cfg = &mut ctx.accounts.config;
        cfg.admin = ctx.accounts.admin.key();
        cfg.oracle = oracle;
        Ok(())
    }

    pub fn set_oracle(ctx: Context<SetOracle>, oracle: Pubkey) -> Result<()> {
        let cfg = &mut ctx.accounts.config;
        require_keys_eq!(cfg.admin, ctx.accounts.admin.key(), SolariaError::UnauthorizedAdmin);
        cfg.oracle = oracle;
        Ok(())
    }
}

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
    /// CHECK: oracle must sign; verified against config.oracle
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
    // conservative sizing: Anchor string = 4 + bytes
    pub const MAX_SIZE: usize =
        8 + // intent_id
        32 + // merchant
        8 + // amount
        1 + // status enum
        (4 + 140) + // description
        (4 + 200) + // source tx
        32; // proof hash
}

#[derive(AnchorSerialize, AnchorDeserialize, Clone, PartialEq, Eq)]
pub enum PaymentStatus {
    Pending,
    Settled,
}

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

#[error_code]
pub enum SolariaError {
    #[msg("Description too long")]
    DescriptionTooLong,
    #[msg("Source chain tx too long")]
    SourceTxTooLong,
    #[msg("Intent id mismatch")]
    IntentIdMismatch,
    #[msg("Already settled")]
    AlreadySettled,
    #[msg("Unauthorized oracle")]
    UnauthorizedOracle,
    #[msg("Unauthorized admin")]
    UnauthorizedAdmin,
}
